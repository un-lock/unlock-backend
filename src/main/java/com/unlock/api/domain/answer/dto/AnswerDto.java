package com.unlock.api.domain.answer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @Schema(description = "답변 등록 요청")
    public static class AnswerRequest {
        @NotBlank(message = "답변 내용은 필수입니다.")
        @Schema(description = "답변 내용", example = "너랑 함께라면 어디든 좋아. 특히 어제 갔던 그 카페가 생각나네.")
        private String content;
    }

    @Getter
    @Builder
    @Schema(description = "오늘의 답변 현황 응답")
    public static class TodayAnswerResponse {
        @Schema(description = "나의 답변 정보")
        private MyAnswerDto myAnswer;
        
        @Schema(description = "상대방의 답변 정보")
        private PartnerAnswerDto partnerAnswer;
    }

    @Getter
    @Builder
    @Schema(description = "나의 답변 상세")
    public static class MyAnswerDto {
        @Schema(description = "답변 ID", example = "101")
        private Long id;

        @Schema(description = "답변 내용", example = "나는 네 웃는 모습이 제일 좋아.")
        private String content;

        @Schema(description = "작성 시각", example = "2026-02-06T14:30:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @Schema(description = "상대방의 답변 정보")
    public static class PartnerAnswerDto {
        @Schema(description = "답변 ID (미작성 시 null)", example = "102")
        private Long id;

        @Schema(description = "상대방 닉네임", example = "달콤한연인")
        private String nickname;

        @Schema(description = "상대방 답변 내용 (잠긴 경우 'LOCKED' 반환)", example = "LOCKED")
        private String content;

        @Schema(description = "상대방 작성 여부", example = "true")
        private boolean isWritten;

        @Schema(description = "내가 답변을 볼 수 있는 권한 여부", example = "false")
        private boolean isRevealed;

        @Schema(description = "작성 시각", example = "2026-02-06T15:00:00")
        private LocalDateTime createdAt;
    }
}
