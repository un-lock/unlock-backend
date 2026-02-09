package com.unlock.api.domain.couple.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.answer.repository.AnswerRevealRepository;
import com.unlock.api.domain.auth.service.RedisService;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleRequestResponse;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleResponse;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.couple.repository.CoupleRepository;
import com.unlock.api.domain.question.repository.CoupleQuestionRepository;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 커플 매칭 및 관리 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CoupleService {

    private final CoupleRepository coupleRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    
    // 연쇄 삭제를 위한 레포지토리들 주입
    private final AnswerRepository answerRepository;
    private final AnswerRevealRepository answerRevealRepository;
    private final CoupleQuestionRepository coupleQuestionRepository;

    /**
     * 내 커플 정보 및 초대 코드 조회
     */
    @Transactional(readOnly = true)
    public CoupleResponse getCoupleInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getInviteCode() == null) {
            user.setInviteCode(generateInviteCode());
        }

        boolean isConnected = user.getCouple() != null;
        String partnerNickname = null;
        LocalDate startDate = null;

        if (isConnected) {
            Couple couple = user.getCouple();
            User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
            partnerNickname = partner.getNickname();
            startDate = couple.getStartDate();
        }

        return CoupleResponse.builder()
                .inviteCode(user.getInviteCode())
                .isConnected(isConnected)
                .partnerNickname(partnerNickname)
                .startDate(startDate)
                .build();
    }

    /**
     * 커플 연결 신청
     */
    public void requestConnection(Long userId, String inviteCode) {
        User requester = userRepository.findById(userId).get();
        if (requester.getCouple() != null) throw new BusinessException(ErrorCode.ALREADY_CONNECTED);

        User target = userRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (target.getId().equals(userId)) throw new BusinessException(ErrorCode.CANNOT_CONNECT_SELF);
        if (target.getCouple() != null) throw new BusinessException(ErrorCode.PARTNER_ALREADY_CONNECTED);
        if (redisService.getCoupleRequest(target.getId()) != null) throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS);

        redisService.saveCoupleRequest(target.getId(), userId);
    }

    /**
     * 연결 신청 수락 및 커플 생성
     */
    public void acceptConnection(Long userId) {
        String requesterIdStr = redisService.getCoupleRequest(userId);
        if (requesterIdStr == null) throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);

        Long requesterId = Long.parseLong(requesterIdStr);
        User user = userRepository.findById(userId).get();
        User requester = userRepository.findById(requesterId).get();

        Couple couple = Couple.builder()
                .user1(requester)
                .user2(user)
                .startDate(LocalDate.now())
                .build();

        coupleRepository.save(couple);
        user.setCouple(couple);
        requester.setCouple(couple);

        redisService.deleteCoupleRequest(userId);
    }

    /**
     * 커플 연결 해제 (Breakup)
     * - [철저한 파기 정책] 모든 답변, 열람 기록, 배정 기록을 즉시 영구 삭제합니다.
     */
    public void breakup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) {
            throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);
        }

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        log.info("[BREAKUP] 커플(ID:{}) 해제 및 데이터 파기를 시작합니다. 요청자: {}", couple.getId(), user.getNickname());

        // 1. 답변 열람 기록 삭제 (AnswerReveal)
        answerRevealRepository.deleteAllByUser(user);
        answerRevealRepository.deleteAllByUser(partner);

        // 2. 두 유저의 모든 답변 삭제 (Answer)
        answerRepository.deleteAllByUser(user);
        answerRepository.deleteAllByUser(partner);

        // 3. 커플 질문 배정 이력 삭제 (CoupleQuestion)
        coupleQuestionRepository.deleteAllByCouple(couple);

        // 4. 유저 상태 초기화 및 초대 코드 재생성
        user.setCouple(null);
        user.setInviteCode(generateInviteCode()); // 새 코드 부여
        
        partner.setCouple(null);
        partner.setInviteCode(generateInviteCode()); // 새 코드 부여

        // 5. 커플 엔티티 삭제
        coupleRepository.delete(couple);

        log.info("[BREAKUP] 커플(ID:{})의 모든 데이터가 성공적으로 파기되었습니다.", couple.getId());
        
        // TODO: 파트너에게 해제 알림 발송 (FCM)
    }

    /**
     * 나에게 온 연결 신청 확인
     */
    @Transactional(readOnly = true)
    public CoupleRequestResponse getReceivedRequest(Long userId) {
        String requesterIdStr = redisService.getCoupleRequest(userId);
        if (requesterIdStr == null) return null;

        User requester = userRepository.findById(Long.parseLong(requesterIdStr)).get();
        return CoupleRequestResponse.builder()
                .requesterId(requester.getId())
                .requesterNickname(requester.getNickname())
                .build();
    }

    public void rejectConnection(Long userId) {
        if (redisService.getCoupleRequest(userId) == null) throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        redisService.deleteCoupleRequest(userId);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
