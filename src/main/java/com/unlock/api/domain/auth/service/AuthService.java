package com.unlock.api.domain.auth.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.common.security.jwt.JwtTokenProvider;
import com.unlock.api.domain.auth.dto.AuthDto.LoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.PasswordResetRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SignupRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SocialLoginRequest;
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
    public LoginDto socialLogin(AuthProvider provider, SocialLoginRequest request) {
        SocialAuthService socialAuthService = socialAuthServices.stream()
                .filter(service -> service.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 소셜 로그인입니다."));

        // 앱에서 획득한 Token을 사용하여 즉시 프로필 조회
        SocialProfile profile = socialAuthService.getProfile(request.getToken());

        // 1. 소셜 ID로 사용자 조회 (이미 연동된 경우)
        User user = userRepository.findBySocialIdAndProvider(profile.getSocialId(), profile.getProvider())
                .orElse(null);

        // 2. 소셜 연동 사용자가 없다면, 이메일로 기존 계정 찾기 (통합 시도)
        if (user == null) {
            user = userRepository.findByEmail(profile.getEmail())
                    .map(existingUser -> {
                        // 기존 계정에 소셜 정보를 연결 (통합)
                        existingUser.updateSocialInfo(profile.getSocialId(), profile.getProvider());
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> {
                        // 아예 신규 가입 (소셜 가입은 랜덤 패스워드 설정)
                        return userRepository.save(User.builder()
                                .socialId(profile.getSocialId())
                                .email(profile.getEmail())
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .nickname(profile.getNickname())
                                .provider(profile.getProvider())
                                .inviteCode(generateInviteCode())
                                .build());
                    });
        }

        // [Apple/Google 전용] authorizationCode → refresh_token 교환 후 저장 (탈퇴 시 revoke에 사용)
        if (request.getAuthorizationCode() != null) {
            if (provider == AuthProvider.APPLE) {
                String appleRefreshToken = socialAuthService.exchangeCode(request.getAuthorizationCode());
                if (appleRefreshToken != null) {
                    user.updateAppleRefreshToken(appleRefreshToken);
                }
            } else if (provider == AuthProvider.GOOGLE) {
                String googleRefreshToken = socialAuthService.exchangeCode(request.getAuthorizationCode());
                if (googleRefreshToken != null) {
                    user.updateGoogleRefreshToken(googleRefreshToken);
                }
            }
        }

        if (request.getFcmToken() != null) {
            handleFcmToken(user, request.getFcmToken());
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

    /**
     * 소셜 연동 해제 (탈퇴 시 호출)
     * - 카카오: socialId로 unlink
     * - 애플: appleRefreshToken으로 revoke
     * - 구글: googleRefreshToken으로 revoke
     */
    public void unlinkSocial(User user) {
        if (user.getProvider() == AuthProvider.EMAIL) return;

        String unlinkToken = switch (user.getProvider()) {
            case APPLE  -> user.getAppleRefreshToken();
            case GOOGLE -> user.getGoogleRefreshToken();
            default     -> user.getSocialId(); // KAKAO
        };

        socialAuthServices.stream()
                .filter(service -> service.getProvider() == user.getProvider())
                .findFirst()
                .ifPresent(service -> service.unlink(unlinkToken));
    }

    private void handleFcmToken(User user, String fcmToken) {
        fcmTokenRepository.findByToken(fcmToken)
                .ifPresentOrElse(
                        existingToken -> {
                            // 이미 존재하는 토큰이면 현재 유저로 업데이트하고 마지막 사용 시간 갱신
                            existingToken.updateUser(user);
                            existingToken.updateLastUsed();
                        },
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
