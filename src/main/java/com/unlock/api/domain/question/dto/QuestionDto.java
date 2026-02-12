package com.unlock.api.domain.question.dto;

import com.unlock.api.domain.question.entity.QuestionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 질문 관련 데이터 전송 객체
 */
public class QuestionDto {

    @Getter
    @Builder
    @Schema(description = "오늘의 질문 응답 객체")
    public static class QuestionResponse {
        
        @Schema(description = "질문 고유 ID", example = "1")
        private Long id;

        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 나를 생각나게 했던 순간은 언제야?")
        private String content;

        @Schema(description = "질문 카테고리", implementation = QuestionCategory.class, example = "DAILY")
        private QuestionCategory category;

        @Schema(description = "현재 사용자의 답변 완료 여부", example = "true")
        private boolean isAnswered;
    }
}