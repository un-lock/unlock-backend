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
        
        // TODO: [Push Notification] target 유저에게 "A님으로부터 커플 연결 신청이 왔습니다! 💌" 알림 발송
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

        // TODO: [Push Notification] requester 유저에게 "신청을 수락하여 커플 연결이 완료되었습니다! 💕" 알림 발송
    }

    /**
     * 커플 연결 해제 (Breakup)
     */
    public void breakup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        answerRevealRepository.deleteAllByUser(user);
        answerRevealRepository.deleteAllByUser(partner);
        answerRepository.deleteAllByUser(user);
        answerRepository.deleteAllByUser(partner);
        coupleQuestionRepository.deleteAllByCouple(couple);

        user.setCouple(null);
        user.setInviteCode(generateInviteCode());
        partner.setCouple(null);
        partner.setInviteCode(generateInviteCode());

        coupleRepository.delete(couple);

        // TODO: [Push Notification] partner 유저에게 "커플 연결이 해제되어 모든 기록이 파기되었습니다. 💔" 알림 발송
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
        String requesterIdStr = redisService.getCoupleRequest(userId);
        if (requesterIdStr == null) throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        
        Long requesterId = Long.parseLong(requesterIdStr);
        
        redisService.deleteCoupleRequest(userId);

        // TODO: [Push Notification] requester 유저에게 "커플 연결 신청이 거절되었습니다. 😢" 알림 발송
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}