package com.unlock.api.domain.auth.service;

import com.unlock.api.common.security.jwt.JwtTokenProvider;
import com.unlock.api.domain.auth.dto.AuthDto.TokenResponse;
import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final List<SocialAuthService> socialAuthServices;

    public TokenResponse socialLogin(AuthProvider provider, String token) {
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
                        .build()));

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .nickname(user.getNickname())
                .isCoupleConnected(user.getCouple() != null)
                .build();
    }
}