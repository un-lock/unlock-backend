package com.unlock.api.domain.answer.repository;

import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.couple.entity.Couple;
import java.util.List;

/**
 * Answer 관련 커스텀 쿼리 인터페이스 (Querydsl용)
 * 이름 규칙: [Repository명] + Custom
 */
public interface AnswerRepositoryCustom {
    
    /**
     * 특정 커플의 월별 질문 및 답변 여부 요약을 DTO로 직접 조회합니다.
     */
    List<ArchiveSummaryResponse> findMonthlyArchiveSummary(Couple couple, Long userId, Long partnerId, int year, int month);

    /**
     * 특정 커플의 전체 질문 아카이브를 페이징 처리하여 조회합니다.
     * @param sortDirection "DESC" (최신순) 또는 "ASC" (오래된순)
     */
    List<ArchiveSummaryResponse> findArchiveList(Couple couple, Long userId, Long partnerId, int page, int size, String sortDirection);
}
