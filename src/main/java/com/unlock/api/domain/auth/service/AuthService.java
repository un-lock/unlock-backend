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
     */
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 존재하는 이메일입니다.", ErrorCode.INVALID_INPUT_VALUE);
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
     */
    public LoginDto login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("이메일 또는 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_INPUT_VALUE));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("이메일 또는 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        return createTokenResponse(user);
    }

    /**
     * 소셜 로그인
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
     * 토큰 재발급
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
     */
    public void logout(Long userId) {
        redisService.deleteRefreshToken(userId);
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

        // Refresh Token Redis 저장
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
}