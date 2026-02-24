package com.unlock.api.domain.couple.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 커플 관련 데이터 전송 객체
 */
public class CoupleDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커플 연결 신청 요청")
    public static class ConnectRequest {
        @NotBlank(message = "초대 코드는 필수입니다.")
        @Schema(description = "상대방의 초대 코드", example = "ABC123DE")
        private String inviteCode;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 시간 변경 요청")
    public static class NotificationTimeRequest {
        @NotNull(message = "알림 시간은 필수입니다.")
        @Schema(description = "변경할 알림 시간 (HH:mm)", example = "22:00")
        private LocalTime notificationTime;
    }

    @Getter
    @Builder
    @Schema(description = "내 커플 정보 응답")
    public static class CoupleResponse {
        @Schema(description = "나의 초대 코드", example = "XYZ789FG")
        private String inviteCode;

        @Schema(description = "커플 연결 여부", example = "true")
        private boolean isConnected;

        @Schema(description = "상대방 닉네임 (연결 안된 경우 null)", example = "달콤한연인")
        private String partnerNickname;

        @Schema(description = "커플 시작일 (연결 안된 경우 null)", example = "2026-02-06")
        private LocalDate startDate;

        @Schema(description = "현재 설정된 알림 시간", example = "21:00")
        private LocalTime notificationTime;
    }

    @Getter
    @Builder
    @Schema(description = "받은 연결 신청 정보")
    public static class CoupleRequestResponse {
        @Schema(description = "신청자 고유 ID", example = "1")
        private Long requesterId;

        @Schema(description = "신청자 닉네임", example = "설레는시작")
        private String requesterNickname;
    }
}