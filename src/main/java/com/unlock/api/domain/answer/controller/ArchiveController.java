package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveDetailResponse;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 아카이브 목록 조회 (캘린더용)
     */
    @GetMapping
    public ApiResponse<List<ArchiveSummaryResponse>> getArchiveList(@CurrentUser Long userId) {
        return ApiResponse.success("아카이브 목록 조회 성공", archiveService.getArchiveList(userId));
    }

    /**
     * 아카이브 상세 조회
     */
    @GetMapping("/{questionId}")
    public ApiResponse<ArchiveDetailResponse> getArchiveDetail(
            @CurrentUser Long userId,
            @PathVariable Long questionId) {
        return ApiResponse.success("아카이브 상세 조회 성공", archiveService.getArchiveDetail(userId, questionId));
    }
}
