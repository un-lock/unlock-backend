package com.unlock.api.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 질문 관련 데이터 전송 객체
 */
public class QuestionDto {

    @Getter
    @Builder
    @Schema(description = "질문 응답 객체")
    public static class QuestionResponse {
        @Schema(description = "질문 고유 ID", example = "1")
        private Long id;
        
        @Schema(description = "질문 내용", example = "우리가 처음 만났을 때, 나의 어떤 점에 가장 끌렸어?")
        private String content;
        
        @Schema(description = "질문 카테고리", example = "로맨틱")
        private String category;
        
        @Schema(description = "현재 유저의 답변 작성 여부", example = "false")
        private boolean isAnswered;
    }
}