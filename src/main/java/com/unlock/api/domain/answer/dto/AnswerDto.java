package com.unlock.api.domain.answer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 답변 관련 데이터 전송 객체
 */
public class AnswerDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "답변 등록 요청 객체")
    public static class AnswerRequest {
        @Schema(description = "답변 내용", example = "나의 첫인상은 정말 따뜻했어!")
        private String content;
    }

    @Getter
    @Builder
    @Schema(description = "오늘의 답변 현황 응답 객체")
    public static class TodayAnswerResponse {
        @Schema(description = "나의 답변 정보")
        private MyAnswerDto myAnswer;
        
        @Schema(description = "파트너의 답변 정보")
        private PartnerAnswerDto partnerAnswer;
    }

    @Getter
    @Builder
    @Schema(description = "나의 답변 정보 객체")
    public static class MyAnswerDto {
        @Schema(description = "답변 ID", example = "1")
        private Long id;
        
        @Schema(description = "답변 내용", example = "나의 첫인상은 정말 따뜻했어!")
        private String content;
        
        @Schema(description = "작성 시각", example = "2026-02-06T14:30:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @Schema(description = "파트너의 답변 정보 객체")
    public static class PartnerAnswerDto {
        @Schema(description = "답변 ID", example = "2")
        private Long id;
        
        @Schema(description = "파트너 닉네임", example = "깜찍한그대")
        private String nickname;
        
        @Schema(description = "답변 내용 (열람 권한 없으면 'LOCKED')", example = "LOCKED")
        private String content;
        
        @Schema(description = "작성 완료 여부", example = "true")
        private boolean isWritten;
        
        @Schema(description = "열람 가능 여부 (광고시청/구독)", example = "false")
        private boolean isRevealed;
        
        @Schema(description = "작성 시각 (미작성 시 null)", example = "2026-02-06T15:00:00")
        private LocalDateTime createdAt;
    }
}