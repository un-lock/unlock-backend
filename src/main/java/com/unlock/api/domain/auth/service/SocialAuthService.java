package com.unlock.api.domain.auth.service;

import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;

public interface SocialAuthService {
    SocialProfile getProfileByCode(String code);
    SocialProfile getProfile(String token);
    AuthProvider getProvider();
}
