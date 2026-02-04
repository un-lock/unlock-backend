package com.unlock.api.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    public static class EmailRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    public static class VerifyRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "인증번호는 필수입니다.")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String nickname;
        private boolean isCoupleConnected;
    }

    @Getter
    @NoArgsConstructor
    public static class SocialLoginRequest {
        @NotBlank(message = "소셜 토큰은 필수입니다.")
        private String token;
    }
}
