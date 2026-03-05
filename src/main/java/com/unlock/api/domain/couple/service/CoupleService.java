package com.unlock.api.domain.couple.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.answer.repository.AnswerRevealRepository;
import com.unlock.api.domain.auth.entity.NotificationType;
import com.unlock.api.domain.auth.service.FcmService;
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
import java.time.LocalTime;
import java.util.UUID;

/**
 * 커플 매칭 및 관계 관리 비즈니스 로직 서비스
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
    private final FcmService fcmService;

    /**
     * 내 커플 정보 및 초대 코드 조회
     */
    @Transactional(readOnly = true)
    public CoupleResponse getCoupleInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 초대 코드가 없는 신규 유저에게 코드 부여
        if (user.getInviteCode() == null) {
            user.updateInviteCode(generateInviteCode());
        }

        boolean isConnected = user.getCouple() != null;
        String partnerNickname = null;
        LocalDate startDate = null;
        LocalTime notificationTime = null;

        if (isConnected) {
            Couple couple = user.getCouple();
            User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
            partnerNickname = partner.getNickname();
            startDate = couple.getStartDate();
            notificationTime = couple.getNotificationTime();
        }

        return CoupleResponse.builder()
                .inviteCode(user.getInviteCode())
                .isConnected(isConnected)
                .partnerNickname(partnerNickname)
                .startDate(startDate)
                .notificationTime(notificationTime)
                .build();
    }

    /**
     * 커플 알림 시간 변경
     */
    public void updateNotificationTime(Long userId, LocalTime notificationTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) {
            throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);
        }

        couple.updateNotificationTime(notificationTime);
        log.info("[UPDATE] 커플(ID:{}) 알림 시간 변경 -> {}", couple.getId(), notificationTime);
    }

    /**
     * 커플 연결 신청
     * 초대 코드를 통해 상대방에게 연결을 요청하며, 정보는 Redis에 24시간 동안 유지됩니다.
     * 
     * @param userId 신청을 보내는 유저 ID
     * @param inviteCode 상대방의 초대 코드
     */
    public void requestConnection(Long userId, String inviteCode) {
        User requester = userRepository.findById(userId).get();
        
        // 1. 본인이 이미 커플인지 확인
        if (requester.getCouple() != null) {
            throw new BusinessException(ErrorCode.ALREADY_CONNECTED);
        }

        // 2. 초대 코드의 유효성 및 대상 유저 존재 확인
        User target = userRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        // 3. 자기 자신과의 연결 방지
        if (target.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_CONNECT_SELF);
        }

        // 4. 상대방이 이미 커플인지 확인
        if (target.getCouple() != null) {
            throw new BusinessException(ErrorCode.PARTNER_ALREADY_CONNECTED);
        }

        // 5. 상대방에게 이미 온 신청이 있는지 확인 (선착순 처리)
        if (redisService.getCoupleRequest(target.getId()) != null) {
            throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS);
        }

        // Redis에 신청 정보 저장 (키: "CP_REQ:상대ID", 값: "내ID")
        redisService.saveCoupleRequest(target.getId(), userId);
        
        // [Push Notification] 상대방에게 연결 신청 알림 발송
        fcmService.sendToUser(target, "un:lock 💌", requester.getNickname() + "님으로부터 커플 연결 신청이 왔습니다!", NotificationType.COUPLE_REQUEST);
    }

    /**
     * 연결 신청 수락
     * 신청 기록을 확인하여 실제 Couple 엔티티를 생성하고 관계를 확정합니다.
     */
    public void acceptConnection(Long userId) {
        // 1. 나에게 온 신청이 있는지 확인
        String requesterIdStr = redisService.getCoupleRequest(userId);
        if (requesterIdStr == null) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        }

        Long requesterId = Long.parseLong(requesterIdStr);
        User user = userRepository.findById(userId).get();
        User requester = userRepository.findById(requesterId).get();

        // 2. Couple 엔티티 생성 및 양방향 연관관계 설정
        Couple couple = Couple.builder()
                .user1(requester)
                .user2(user)
                .startDate(LocalDate.now())
                .build();

        coupleRepository.save(couple);
        user.setCouple(couple);
        requester.setCouple(couple);

        // 3. 처리 완료된 Redis 신청 정보 삭제
        redisService.deleteCoupleRequest(userId);

        // [Push Notification] 신청자에게 연결 완료 알림 발송
        fcmService.sendToUser(requester, "un:lock 💕", user.getNickname() + "님이 신청을 수락하여 커플 연결이 완료되었습니다!", NotificationType.COUPLE_CONNECTED);
    }

    /**
     * 커플 연결 해제 (Breakup)
     * 은밀한 대화 서비스의 특성상, 해제 시 모든 관련 데이터를 즉시 영구 파기(Hard Delete) 합니다.
     * 파기 대상: 모든 답변, 열람 기록, 질문 배정 기록
     */
    public void breakup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) {
            throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);
        }

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        log.info("[BREAKUP] 커플(ID:{}) 데이터 영구 파기 시작 (요청자: {})", couple.getId(), user.getNickname());

        // 1. 연관 데이터 삭제 (순서 준수)
        answerRevealRepository.deleteAllByUser(user);
        answerRevealRepository.deleteAllByUser(partner);
        answerRepository.deleteAllByUser(user);
        answerRepository.deleteAllByUser(partner);
        coupleQuestionRepository.deleteAllByCouple(couple);

        // 2. 유저 관계 초기화 및 신규 초대 코드 부여 (새 인연을 위해)
        user.setCouple(null);
        user.updateInviteCode(generateInviteCode());
        
        partner.setCouple(null);
        partner.updateInviteCode(generateInviteCode());

        // 3. 커플 엔티티 삭제
        coupleRepository.delete(couple);

        log.info("[BREAKUP] 커플(ID:{})의 모든 기록이 성공적으로 삭제되었습니다.", couple.getId());
        
        // [Push Notification] partner 유저에게 "커플 연결이 해제되어 모든 기록이 파기되었습니다. 💔" 알림 발송
        fcmService.sendToUser(partner, "un:lock 💔", "커플 연결이 해제되어 모든 기록이 파기되었습니다.", NotificationType.COUPLE_DISCONNECTED);
    }

    /**
     * 나에게 온 연결 신청 정보 확인
     * @return 신청자 ID와 닉네임 정보를 담은 DTO
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

    /**
     * 연결 신청 거절
     */
    public void rejectConnection(Long userId) {
        String requesterIdStr = redisService.getCoupleRequest(userId);
        if (requesterIdStr == null) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        }
        
        Long requesterId = Long.parseLong(requesterIdStr);
        User user = userRepository.findById(userId).get();
        User requester = userRepository.findById(requesterId).get();

        redisService.deleteCoupleRequest(userId);

        // [Push Notification] 신청자에게 거절 알림 발송
        fcmService.sendToUser(requester, "un:lock 😢", user.getNickname() + "님이 커플 연결 신청을 거절하였습니다.", NotificationType.COUPLE_REQUEST_REJECTED);
    }

    /**
     * 8자리 대문자 초대 코드 생성
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
