package com.unlock.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 관련 데이터 전송 객체
 */
public class UserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NicknameUpdateRequest {
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 20, message = "닉네임은 1자에서 20자 사이여야 합니다.")
        private String nickname;
    }
}
