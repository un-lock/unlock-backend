package com.unlock.api.domain.user.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.auth.service.AuthService;
import com.unlock.api.domain.couple.service.CoupleService;
import com.unlock.api.domain.user.dto.UserDto.NicknameUpdateRequest;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 관리(닉네임 변경, 탈퇴 등) 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CoupleService coupleService;
    private final AuthService authService;

    /**
     * 닉네임 변경
     * - 중복 허용 정책에 따라 즉시 변경
     */
    public String updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateNickname(request.getNickname());
        log.info("유저(ID:{}) 닉네임 변경 완료: {}", userId, request.getNickname());
        return user.getNickname();
    }

    /**
     * 회원 탈퇴
     * 1. 커플 상태인 경우 커플 해제 및 데이터 파기 선행
     * 2. 유저 엔티티 삭제
     * 3. 인증 토큰(Redis) 파기
     */
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. 커플인 경우 데이터 싹 파기
        if (user.getCouple() != null) {
            log.info("탈퇴 유저(ID:{}) 커플 상태 확인 -> 데이터 파기 프로세스 시작", userId);
            coupleService.breakup(userId);
        }

        // 2. 인증 정보 삭제 (로그아웃 처리)
        authService.logout(userId);

        // 3. 유저 삭제
        userRepository.delete(user);
        log.info("유저(ID:{}) 회원 탈퇴 완료. 모든 데이터가 삭제되었습니다.", userId);
    }
}
