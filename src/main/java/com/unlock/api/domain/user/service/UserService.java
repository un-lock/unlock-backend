package com.unlock.api.domain.user.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.answer.repository.AnswerRevealRepository;
import com.unlock.api.domain.auth.service.AuthService;
import com.unlock.api.domain.couple.service.CoupleService;
import com.unlock.api.domain.user.dto.UserDto.NicknameUpdateRequest;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserFcmTokenRepository;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 관리 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CoupleService coupleService;
    private final AuthService authService;
    private final AnswerRepository answerRepository;
    private final AnswerRevealRepository answerRevealRepository;
    private final UserFcmTokenRepository fcmTokenRepository;

    /**
     * 닉네임 변경
     */
    public String updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateNickname(request.getNickname());
        log.info("유저(ID:{}) 닉네임 변경 완료: {}", userId, request.getNickname());
        return user.getNickname();
    }

    /**
     * 회원 탈퇴 (데이터 파기)
     */
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("[WITHDRAW] 유저(ID:{}, 닉네임:{}) 회원 탈퇴 및 데이터 영구 파기 시작", userId, user.getNickname());

        // 1. 커플인 경우 커플 관련 데이터(상대방과의 연결, 배정 질문 등) 선제 파기
        if (user.getCouple() != null) {
            coupleService.breakup(userId);
        }

        // 2. 유저 개인 데이터 연쇄 파기 (커플이 아니었더라도 남아있을 수 있는 데이터 정리)
        answerRevealRepository.deleteAllByUser(user);
        answerRepository.deleteAllByUser(user);
        fcmTokenRepository.deleteAllByUser(user);

        // 3. 인증 정보(Redis RefreshToken) 완전 파기
        authService.logout(userId, null);

        // 4. 최종 유저 엔티티 삭제 (DB 제약 조건 문제 해결)
        userRepository.delete(user);
        
        log.info("[WITHDRAW] 유저(ID:{}) 모든 데이터가 성공적으로 파기되었습니다.", userId);
    }
}
