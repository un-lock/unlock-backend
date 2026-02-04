package com.unlock.api.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 DTO 모음
 */
public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        private String email;

        @NotBlank(message = "인증번호는 필수 입력 항목입니다.")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignupRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        private String password;

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        private String password;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReissueRequest {
        @NotBlank(message = "RefreshToken은 필수입니다.")
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLoginRequest {
        @NotBlank(message = "토큰은 필수입니다.")
        private String token;
    }

        @Getter

        @Builder

        @AllArgsConstructor

        public static class TokenResponse {

            private String accessToken;

            private String nickname;

            private boolean isCoupleConnected;

        }

    }

    