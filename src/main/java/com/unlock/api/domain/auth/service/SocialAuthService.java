package com.unlock.api.domain.auth.service;

import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;

public interface SocialAuthService {
    SocialProfile getProfile(String token);
    void unlink(String token);
    AuthProvider getProvider();

    /**
     * authorizationCode를 소셜 플랫폼의 refresh_token으로 교환합니다.
     * Apple 전용이며, 다른 소셜 제공자는 기본적으로 null을 반환합니다.
     */
    default String exchangeCode(String authorizationCode) {
        return null;
    }
}
