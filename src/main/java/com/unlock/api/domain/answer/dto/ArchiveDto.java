package com.unlock.api.domain.answer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 아카이브(기록장) 관련 데이터 전송 객체
 */
public class ArchiveDto {

    @Getter
    @Builder
    @Schema(description = "아카이브 요약 응답 객체 (캘린더용)")
    public static class ArchiveSummaryResponse {
        @Schema(description = "질문 고유 ID", example = "1")
        private Long questionId;
        
        @Schema(description = "질문 내용", example = "우리가 처음 만났을 때...")
        private String questionContent;
        
        @Schema(description = "배정된 날짜", example = "2026-02-06")
        private LocalDate date;
        
        @Schema(description = "나의 답변 완료 여부", example = "true")
        private boolean myAnswered;
        
        @Schema(description = "파트너의 답변 완료 여부", example = "true")
        private boolean partnerAnswered;
    }

    @Getter
    @Builder
    @Schema(description = "아카이브 상세 응답 객체")
    public static class ArchiveDetailResponse {
        @Schema(description = "질문 내용", example = "우리가 처음 만났을 때...")
        private String questionContent;
        
        @Schema(description = "질문 카테고리", example = "로맨틱")
        private String category;
        
        @Schema(description = "배정된 날짜", example = "2026-02-06")
        private LocalDate date;
        
        @Schema(description = "나의 답변 상세")
        private AnswerDto.MyAnswerDto myAnswer;
        
        @Schema(description = "파트너의 답변 상세 (잠금 처리 포함)")
        private AnswerDto.PartnerAnswerDto partnerAnswer;
    }
}