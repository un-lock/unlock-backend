package com.unlock.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 데이터 전송 객체
 */
public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "이메일 인증 요청 객체")
    public static class EmailRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Schema(description = "사용자 이메일", example = "couple@example.com")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증번호 확인 요청 객체")
    public static class VerifyRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Schema(description = "사용자 이메일", example = "couple@example.com")
        private String email;

        @NotBlank(message = "인증번호는 필수 입력 항목입니다.")
        @Schema(description = "이메일로 발송된 6자리 인증번호", example = "123456")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "이메일 회원가입 요청 객체")
    public static class SignupRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Schema(description = "사용자 이메일", example = "couple@example.com")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        @Schema(description = "비밀번호 (최소 8자)", example = "password123!")
        private String password;

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Schema(description = "사용자 닉네임", example = "달콤한연인")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "로그인 요청 객체")
    public static class LoginRequest {
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Schema(description = "사용자 이메일", example = "couple@example.com")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Schema(description = "비밀번호", example = "password123!")
        private String password;

        @Schema(description = "FCM 기기 토큰 (알림용)", example = "fcm_token_sample_xyz")
        private String fcmToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 찾기(인증번호 요청) 객체")
    public static class PasswordFindRequest {
        @Email @NotBlank
        @Schema(description = "가입한 이메일 주소", example = "couple@example.com")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "임시 비밀번호 발급 요청 객체")
    public static class PasswordResetRequest {
        @Email @NotBlank
        private String email;
        
        @NotBlank
        @Schema(description = "이메일로 발송된 인증번호", example = "123456")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "로그아웃 요청 객체")
    public static class LogoutRequest {
        @Schema(description = "로그아웃할 기기의 FCM 토큰", example = "fcm_token_sample_xyz")
        private String fcmToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "토큰 재발급 요청 객체")
    public static class ReissueRequest {
        @NotBlank(message = "RefreshToken은 필수입니다.")
        @Schema(description = "발급받았던 RefreshToken")
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "소셜 로그인 요청 객체")
    public static class SocialLoginRequest {
        @NotBlank(message = "토큰은 필수입니다.")
        @Schema(description = "소셜 플랫폼(Kakao 등)에서 발급받은 AccessToken")
        private String token;

        @Schema(description = "FCM 기기 토큰 (알림용)", example = "fcm_token_sample_xyz")
        private String fcmToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "로그인 성공 응답 객체")
    public static class TokenResponse {
        @Schema(description = "서비스 전용 AccessToken")
        private String accessToken;
        
        @Schema(description = "사용자 닉네임", example = "달콤한연인")
        private String nickname;
        
        @Schema(description = "커플 연결 여부", example = "false")
        private boolean isCoupleConnected;
    }
}
