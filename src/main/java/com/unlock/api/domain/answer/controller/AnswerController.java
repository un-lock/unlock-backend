package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiCommonResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.AnswerDto.AnswerRequest;
import com.unlock.api.domain.answer.dto.AnswerDto.TodayAnswerResponse;
import com.unlock.api.domain.answer.service.AnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 답변 관련 API 컨트롤러
 */
@Tag(name = "4. Answer", description = "답변 등록 및 파트너 답변 잠금 해제(Unlock) API")
@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "오늘의 답변 등록")
    @ApiResponse(responseCode = "200", description = "등록 성공")
    @PostMapping
    public ApiCommonResponse<Void> submitAnswer(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestBody @Valid AnswerRequest request) {
        answerService.submitAnswer(userId, request);
        return ApiCommonResponse.success("답변이 등록되었습니다.", null);
    }

    @Operation(summary = "오늘의 답변 현황 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = TodayAnswerResponse.class)))
    @GetMapping("/today")
    public ApiCommonResponse<TodayAnswerResponse> getTodayAnswers(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiCommonResponse.success("오늘의 답변 조회 성공", answerService.getTodayAnswers(userId));
    }

    @Operation(summary = "파트너 답변 잠금 해제 (Reveal)")
    @ApiResponse(responseCode = "200", description = "공개 성공")
    @PostMapping("/{answerId}/reveal")
    public ApiCommonResponse<Void> revealPartnerAnswer(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long answerId) {
        answerService.revealPartnerAnswer(userId, answerId);
        return ApiCommonResponse.success("상대방의 답변이 공개되었습니다.", null);
    }
}
