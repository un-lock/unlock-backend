package com.unlock.api.domain.answer.dto;

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
    public static class AnswerRequest {
        private String content;
    }

    @Getter
    @Builder
    public static class TodayAnswerResponse {
        private MyAnswerDto myAnswer;
        private PartnerAnswerDto partnerAnswer;
    }

    @Getter
    @Builder
    public static class MyAnswerDto {
        private Long id;
        private String content;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class PartnerAnswerDto {
        private Long id;
        private String nickname;
        private String content; // isRevealed가 false면 "LOCKED" 반환
        private boolean isWritten; // 상대방이 작성했는지 여부
        private boolean isRevealed; // 내가 내용을 볼 수 있는지 여부
        private LocalDateTime createdAt;
    }
}
