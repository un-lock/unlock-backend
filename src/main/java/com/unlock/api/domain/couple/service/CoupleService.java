package com.unlock.api.domain.couple.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.auth.service.RedisService;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleRequestResponse;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleResponse;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.couple.repository.CoupleRepository;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 커플 매칭 및 관리 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CoupleService {

    private final CoupleRepository coupleRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    /**
     * 내 커플 정보 및 초대 코드 조회
     */
    @Transactional(readOnly = true)
    public CoupleResponse getCoupleInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 초대 코드가 없으면 생성하여 저장 (최초 1회)
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
     * 커플 연결 신청 (초대 코드 사용)
     * - 상대방의 코드를 확인하고 Redis에 신청 상태 저장
     */
    public void requestConnection(Long userId, String inviteCode) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (requester.getCouple() != null) {
            throw new BusinessException(ErrorCode.ALREADY_CONNECTED);
        }

        User target = userRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (target.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_CONNECT_SELF);
        }

        if (target.getCouple() != null) {
            throw new BusinessException(ErrorCode.PARTNER_ALREADY_CONNECTED);
        }

        // 상대방에게 이미 온 신청이 있는지 확인 (방어적 설계)
        if (redisService.getCoupleRequest(target.getId()) != null) {
            throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS);
        }

        // Redis에 신청 정보 저장 (Target ID를 키로, Requester ID를 값으로)
        redisService.saveCoupleRequest(target.getId(), userId);
    }

    /**
     * 나에게 온 연결 신청 확인
     */
    @Transactional(readOnly = true)
    public CoupleRequestResponse getReceivedRequest(Long userId) {
        String requesterIdStr = redisService.getCoupleRequest(userId);
        
        if (requesterIdStr == null) {
            return null; // 온 신청이 없음
        }

        Long requesterId = Long.parseLong(requesterIdStr);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return CoupleRequestResponse.builder()
                .requesterId(requester.getId())
                .requesterNickname(requester.getNickname())
                .build();
    }

    /**
     * 연결 신청 수락
     */
    public void acceptConnection(Long userId) {
        String requesterIdStr = redisService.getCoupleRequest(userId);
        if (requesterIdStr == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // 혹은 적절한 에러코드
        }

        Long requesterId = Long.parseLong(requesterIdStr);
        User user = userRepository.findById(userId).get();
        User requester = userRepository.findById(requesterId).get();

        // Couple 엔티티 생성 및 실제 연결
        Couple couple = Couple.builder()
                .user1(requester)
                .user2(user)
                .startDate(LocalDate.now())
                .build();

        coupleRepository.save(couple);
        user.setCouple(couple);
        requester.setCouple(couple);

        // Redis 신청 정보 삭제
        redisService.deleteCoupleRequest(userId);
    }

    /**
     * 연결 신청 거절
     */
    public void rejectConnection(Long userId) {
        redisService.deleteCoupleRequest(userId);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
