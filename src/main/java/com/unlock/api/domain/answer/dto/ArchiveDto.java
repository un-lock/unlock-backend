package com.unlock.api.domain.answer.dto;

import com.unlock.api.domain.question.entity.QuestionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 아카이브(기록장) 관련 데이터 전송 객체
 */
public class ArchiveDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아카이브 월별 요약 정보")
    public static class ArchiveSummaryResponse {
        @Schema(description = "질문 고유 ID", example = "10")
        private Long questionId;

        @Schema(description = "질문 내용 요약", example = "상대방의 신체 부위 중 가장 매력적인 곳은?")
        private String questionContent;

        @Schema(description = "배정된 날짜", example = "2026-02-06")
        private LocalDate date;

        @Schema(description = "나의 답변 여부", example = "true")
        private Boolean myAnswered; // Querydsl BooleanExpression 결과에 맞춰 Boolean으로 설정

        @Schema(description = "파트너의 답변 여부", example = "true")
        private Boolean partnerAnswered;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아카이브 상세 정보 (질문 및 두 사람의 답변)")
    public static class ArchiveDetailResponse {
        @Schema(description = "질문 내용", example = "우리가 싸웠을 때, 내가 어떻게 화해를 청하면 기분이 풀릴 것 같아?")
        private String questionContent;

        @Schema(description = "질문 카테고리", example = "DEEP_TALK")
        private QuestionCategory category;

        @Schema(description = "배정된 날짜", example = "2026-02-06")
        private LocalDate date;

        @Schema(description = "나의 답변 정보")
        private AnswerDto.MyAnswerDto myAnswer;

        @Schema(description = "파트너의 답변 정보")
        private AnswerDto.PartnerAnswerDto partnerAnswer;
    }
}