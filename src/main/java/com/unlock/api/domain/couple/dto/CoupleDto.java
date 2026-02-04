package com.unlock.api.domain.couple.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class CoupleDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoupleResponse {
        private String inviteCode;
        private boolean isConnected;
        private String partnerNickname;
        private LocalDate startDate;
    }

    @Getter
    @NoArgsConstructor
    public static class ConnectRequest {
        @NotBlank(message = "초대 코드는 필수입니다.")
        private String inviteCode;
    }
}
