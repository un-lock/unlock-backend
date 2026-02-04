package com.unlock.api.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 로그인 수단 (인증 제공자)
 */
@Getter
@RequiredArgsConstructor
public enum AuthProvider {
    KAKAO("KAKAO"),
    GOOGLE("GOOGLE"),
    APPLE("APPLE"),
    EMAIL("EMAIL");

    private final String name;
}
