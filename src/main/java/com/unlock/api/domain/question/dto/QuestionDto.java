package com.unlock.api.domain.question.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 질문 관련 데이터 전송 객체
 */
public class QuestionDto {

    @Getter
    @Builder
    public static class QuestionResponse {
        private Long id;
        private String content;
        private String category; // 질문 카테고리 (설명)
        private boolean isAnswered; // 현재 유저가 답변을 작성했는지 여부
    }
}
