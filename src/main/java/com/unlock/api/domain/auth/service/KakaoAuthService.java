package com.unlock.api.domain.auth.service;

import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoAuthService implements SocialAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public SocialProfile getProfile(String token) {
        // Kakao API 호출하여 프로필 정보 가져오기 (실제 로직)
        // URL: https://kapi.kakao.com/v2/user/me
        // Authorization: Bearer {token}
        
        // Mocking for now - 실제 구현 시 RestTemplate/WebClient 사용
        return SocialProfile.builder()
                .socialId("kakao_mock_id")
                .email("kakao@example.com")
                .nickname("카카오유저")
                .provider(AuthProvider.KAKAO)
                .build();
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }
}
