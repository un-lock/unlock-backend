package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveDetailResponse;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 아카이브(기록장) 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 월별 아카이브 요약 조회 (캘린더 점 찍기용)
     * @param year 년도 (ex: 2026)
     * @param month 월 (ex: 2)
     */
    @GetMapping
    public ApiResponse<List<ArchiveSummaryResponse>> getMonthlyArchive(
            @CurrentUser Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success("월별 아카이브 조회 성공", archiveService.getMonthlyArchive(userId, year, month));
    }

    /**
     * 아카이브 상세 조회 (특정 질문 클릭 시)
     */
    @GetMapping("/{questionId}")
    public ApiResponse<ArchiveDetailResponse> getArchiveDetail(
            @CurrentUser Long userId,
            @PathVariable Long questionId) {
        return ApiResponse.success("아카이브 상세 조회 성공", archiveService.getArchiveDetail(userId, questionId));
    }
}