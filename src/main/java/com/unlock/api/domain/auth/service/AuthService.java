package com.unlock.api.domain.auth.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.common.security.jwt.JwtTokenProvider;
import com.unlock.api.domain.auth.dto.AuthDto.LoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SignupRequest;
import com.unlock.api.domain.auth.dto.AuthDto.TokenResponse;
import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 인증(회원가입, 로그인, 토큰 관리) 관련 비즈니스 로직 담당 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final List<SocialAuthService> socialAuthServices;

    /**
     * 이메일 회원가입
     * - 중복 이메일 체크
     * - 비밀번호 암호화 저장
     * - 가입 시 초대 코드 자동 생성
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
     */
    public LoginDto login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return createTokenResponse(user);
    }

    /**
     * 소셜 로그인 (Kakao, Google, Apple)
     * - 소셜 프로필 정보 조회 후 자동 가입 또는 로그인 처리
     */
    public LoginDto socialLogin(AuthProvider provider, String token) {
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

        return createTokenResponse(user);
    }

    /**
     * JWT 토큰 재발급
     * - 전달받은 Refresh Token 검증
     * - Redis에 저장된 토큰과 대조하여 보안성 강화
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
     * 로그아웃
     * - 서버(Redis)에서 Refresh Token 폐기
     */
    public void logout(Long userId) {
        redisService.deleteRefreshToken(userId);
    }

    /**
     * 서비스 내부용 토큰 및 유저 정보 DTO
     */
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

    /**
     * 토큰 생성 및 Redis 저장 공통 로직
     */
    private LoginDto createTokenResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token Redis 저장 (만료 시간 설정)
        redisService.saveRefreshToken(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenValidityInMilliseconds());

        return LoginDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .nickname(user.getNickname())
                .isCoupleConnected(user.getCouple() != null)
                .build();
    }

    /**
     * 8자리 대문자 초대 코드 생성
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}