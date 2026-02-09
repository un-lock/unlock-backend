package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveDetailResponse;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 아카이브(기록장) 관련 API 컨트롤러
 */
@Tag(name = "5. Archive", description = "과거 질문 및 답변 기록(캘린더) 조회 API")
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @Operation(summary = "월별 아카이브 요약 조회", description = "캘린더 구성을 위해 특정 년/월의 답변 완료 여부 리스트를 조회합니다.")
    @GetMapping
    public ApiResponse<List<ArchiveSummaryResponse>> getMonthlyArchive(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Parameter(description = "조회 년도 (ex: 2026)", example = "2026") @RequestParam int year,
            @Parameter(description = "조회 월 (ex: 2)", example = "2") @RequestParam int month) {
        return ApiResponse.success("월별 아카이브 조회 성공", archiveService.getMonthlyArchive(userId, year, month));
    }

    @Operation(summary = "아카이브 상세 조회", description = "캘린더에서 특정 날짜를 클릭했을 때 당시의 질문과 답변 상세 내용을 조회합니다.")
    @GetMapping("/{questionId}")
    public ApiResponse<ArchiveDetailResponse> getArchiveDetail(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long questionId) {
        return ApiResponse.success("아카이브 상세 조회 성공", archiveService.getArchiveDetail(userId, questionId));
    }
}
