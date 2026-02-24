package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiCommonResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveDetailResponse;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "월별 아카이브 요약 조회 (캘린더용)")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArchiveSummaryResponse.class))))
    @GetMapping
    public ApiCommonResponse<List<ArchiveSummaryResponse>> getMonthlyArchive(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Parameter(description = "조회 년도", example = "2026") @RequestParam int year,
            @Parameter(description = "조회 월", example = "2") @RequestParam int month) {
        return ApiCommonResponse.success("월별 아카이브 조회 성공", archiveService.getMonthlyArchive(userId, year, month));
    }

    @Operation(summary = "아카이브 상세 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ArchiveDetailResponse.class)))
    @GetMapping("/{questionId}")
    public ApiCommonResponse<ArchiveDetailResponse> getArchiveDetail(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long questionId) {
        return ApiCommonResponse.success("아카이브 상세 조회 성공", archiveService.getArchiveDetail(userId, questionId));
    }
}