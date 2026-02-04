package com.unlock.api.domain.couple.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.couple.dto.CoupleDto;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.couple.repository.CoupleRepository;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CoupleService {

    private final CoupleRepository coupleRepository;
    private final UserRepository userRepository;

    /**
     * 내 커플 정보 및 초대 코드 조회
     */
    public CoupleDto.CoupleResponse getCoupleInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 초대 코드가 없으면 생성
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

        return CoupleDto.CoupleResponse.builder()
                .inviteCode(user.getInviteCode())
                .isConnected(isConnected)
                .partnerNickname(partnerNickname)
                .startDate(startDate)
                .build();
    }

    /**
     * 초대 코드로 커플 연결
     */
    public void connect(Long userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getCouple() != null) {
            throw new BusinessException(ErrorCode.ALREADY_CONNECTED);
        }

        User partner = userRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (partner.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_CONNECT_SELF);
        }

        if (partner.getCouple() != null) {
            throw new BusinessException(ErrorCode.PARTNER_ALREADY_CONNECTED);
        }

        // Couple 엔티티 생성 및 연결
        Couple couple = Couple.builder()
                .user1(partner)
                .user2(user)
                .startDate(LocalDate.now())
                .build();

        coupleRepository.save(couple);
        user.setCouple(couple);
        partner.setCouple(couple);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
