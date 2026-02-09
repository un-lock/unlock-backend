package com.unlock.api.domain.answer.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 아카이브(기록장) 관련 데이터 전송 객체
 */
public class ArchiveDto {

    @Getter
    @Builder
    public static class ArchiveSummaryResponse {
        private Long questionId;
        private String questionContent;
        private LocalDate date;
        private boolean myAnswered;
        private boolean partnerAnswered;
    }

    @Getter
    @Builder
    public static class ArchiveDetailResponse {
        private String questionContent;
        private String category;
        private LocalDate date;
        private AnswerDto.MyAnswerDto myAnswer;
        private AnswerDto.PartnerAnswerDto partnerAnswer;
    }
}
