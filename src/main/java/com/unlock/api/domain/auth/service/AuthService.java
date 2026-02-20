package com.unlock.api.domain.auth.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.common.security.jwt.JwtTokenProvider;
import com.unlock.api.domain.auth.dto.AuthDto.LoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.PasswordResetRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SignupRequest;
import com.unlock.api.domain.auth.dto.AuthDto.TokenResponse;
import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.entity.UserFcmToken;
import com.unlock.api.domain.user.repository.UserFcmTokenRepository;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 인증(회원가입, 로그인, 토큰 관리) 및 FCM 토큰 연동 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserFcmTokenRepository fcmTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final EmailService emailService; // 추가
    private final List<SocialAuthService> socialAuthServices;

    /**
     * 이메일 회원가입
     */
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .provider(AuthProvider.EMAIL)
                .inviteCode(generateInviteCode())
                .build();

        userRepository.save(user);
    }

    /**
     * 이메일 로그인
     * - 이메일 존재 여부 및 비밀번호 일치 확인
     * - Access/Refresh Token 세트 발급
     * - FCM 토큰 자동 연동 (추가)
     */
    public LoginDto login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // FCM 토큰 처리
        if (request.getFcmToken() != null) {
            handleFcmToken(user, request.getFcmToken());
        }

        return createTokenResponse(user);
    }

    /**
     * 비밀번호 재설정을 위한 인증번호 발송
     * 가입된 이메일인 경우에만 인증번호를 보냅니다.
     */
    public void requestPasswordResetCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        emailService.sendPasswordResetCode(email);
    }

    /**
     * 인증번호 확인 후 임시 비밀번호 발급
     */
    public void resetPassword(PasswordResetRequest request) {
        // 1. 인증번호 확인
        emailService.verifyCode(request.getEmail(), request.getCode());

        // 2. 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 임시 비밀번호 생성 및 저장 (암호화)
        String tempPassword = generateTemporaryPassword();
        user.updatePassword(passwordEncoder.encode(tempPassword));

        // 4. 이메일 발송
        emailService.sendTemporaryPassword(request.getEmail(), tempPassword);
    }

    /**
     * 소셜 로그인 및 FCM 토큰 등록
     */
    public LoginDto socialLogin(AuthProvider provider, String token, String fcmToken) {
        SocialAuthService socialAuthService = socialAuthServices.stream()
                .filter(service -> service.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 소셜 로그인입니다."));

        SocialProfile profile = socialAuthService.getProfile(token);

        User user = userRepository.findBySocialIdAndProvider(profile.getSocialId(), profile.getProvider())
                .orElseGet(() -> userRepository.save(User.builder()
                        .socialId(profile.getSocialId())
                        .email(profile.getEmail())
                        .nickname(profile.getNickname())
                        .provider(profile.getProvider())
                        .inviteCode(generateInviteCode())
                        .build()));

        if (fcmToken != null) {
            handleFcmToken(user, fcmToken);
        }

        return createTokenResponse(user);
    }

    /**
     * JWT 토큰 재발급
     */
    public LoginDto reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String savedRefreshToken = redisService.getRefreshToken(userId);

        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return createTokenResponse(user);
    }

    /**
     * 로그아웃 및 특정 기기 FCM 토큰 해제
     */
    public void logout(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        redisService.deleteRefreshToken(userId);

        if (fcmToken != null) {
            fcmTokenRepository.deleteByUserAndToken(user, fcmToken);
        }
    }

    private void handleFcmToken(User user, String fcmToken) {
        fcmTokenRepository.findByToken(fcmToken)
                .ifPresentOrElse(
                        UserFcmToken::updateLastUsed,
                        () -> fcmTokenRepository.save(UserFcmToken.builder()
                                .user(user)
                                .token(fcmToken)
                                .lastUsedAt(LocalDateTime.now())
                                .build())
                );
    }

    @Getter
    @Builder
    public static class LoginDto {
        private String accessToken;
        private String refreshToken;
        private String nickname;
        private boolean isCoupleConnected;

        public TokenResponse toTokenResponse() {
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .nickname(nickname)
                    .isCoupleConnected(isCoupleConnected)
                    .build();
        }
    }

    private LoginDto createTokenResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        redisService.saveRefreshToken(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenValidityInMilliseconds());

        return LoginDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .nickname(user.getNickname())
                .isCoupleConnected(user.getCouple() != null)
                .build();
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 임시 비밀번호 생성 (8자리 랜덤 문자열)
     */
    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
