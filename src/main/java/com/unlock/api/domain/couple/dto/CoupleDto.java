package com.unlock.api.domain.couple.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 커플 관련 데이터 전송 객체
 */
public class CoupleDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커플 연결 신청 요청 객체")
    public static class ConnectRequest {
        @NotBlank(message = "초대 코드는 필수입니다.")
        @Schema(description = "상대방의 8자리 초대 코드", example = "ABC123DE")
        private String inviteCode;
    }

    @Getter
    @Builder
    @Schema(description = "커플 정보 응답 객체")
    public static class CoupleResponse {
        @Schema(description = "내 초대 코드", example = "XYZ789FG")
        private String inviteCode;
        
        @Schema(description = "현재 커플 연결 여부", example = "true")
        private boolean isConnected;
        
        @Schema(description = "파트너 닉네임 (미연결 시 null)", example = "달콤한연인")
        private String partnerNickname;
        
        @Schema(description = "커플 시작일 (미연결 시 null)", example = "2026-02-06")
        private LocalDate startDate;
    }

    @Getter
    @Builder
    @Schema(description = "받은 연결 신청 정보 객체")
    public static class CoupleRequestResponse {
        @Schema(description = "신청한 유저의 ID", example = "123")
        private Long requesterId;
        
        @Schema(description = "신청한 유저의 닉네임", example = "깜찍한그대")
        private String requesterNickname;
    }
}
