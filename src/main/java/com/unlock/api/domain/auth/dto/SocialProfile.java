package com.unlock.api.domain.auth.dto;

import com.unlock.api.domain.user.entity.AuthProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialProfile {
    private String socialId;
    private String email;
    private String nickname;
    private AuthProvider provider;
}
