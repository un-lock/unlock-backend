package com.unlock.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 관련 데이터 전송 객체
 */
public class UserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "닉네임 수정 요청")
    public static class NicknameUpdateRequest {
        @NotBlank(message = "닉네임은 필수입니다.")
        @Schema(description = "새로운 닉네임", example = "새로운연인")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 변경 요청")
    public static class PasswordUpdateRequest {
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        @Schema(description = "현재 비밀번호 (또는 임시 비밀번호)", example = "old_password123!")
        private String currentPassword;

        @NotBlank(message = "새로운 비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        @Schema(description = "새로 바꿀 비밀번호", example = "new_password456!")
        private String newPassword;
    }

    @Getter
    @Builder
    @Schema(description = "유저 기본 정보 응답")
    public static class UserResponse {
        @Schema(description = "유저 고유 ID", example = "1")
        private Long id;
        @Schema(description = "이메일", example = "couple@example.com")
        private String email;
        @Schema(description = "닉네임", example = "달콤한연인")
        private String nickname;
    }
}
