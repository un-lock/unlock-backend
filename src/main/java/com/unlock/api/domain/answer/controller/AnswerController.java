package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.AnswerDto.AnswerRequest;
import com.unlock.api.domain.answer.dto.AnswerDto.TodayAnswerResponse;
import com.unlock.api.domain.answer.service.AnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "오늘의 답변 등록", description = "오늘 배정된 질문에 대해 답변을 등록합니다. (중복 등록 불가)")
    @PostMapping
    public ApiResponse<Void> submitAnswer(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestBody @Valid AnswerRequest request) {
        answerService.submitAnswer(userId, request);
        return ApiResponse.success("답변이 등록되었습니다.", null);
    }

    @Operation(summary = "오늘의 답변 현황 조회", description = "나와 파트너의 오늘 답변 상태를 조회합니다. (본인 미작성 시 파트너 답변 차단)")
    @GetMapping("/today")
    public ApiResponse<TodayAnswerResponse> getTodayAnswers(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("오늘의 답변 조회 성공", answerService.getTodayAnswers(userId));
    }

    @Operation(summary = "파트너 답변 잠금 해제 (Reveal)", description = "광고 시청 완료 후 파트너의 답변 잠금을 해제합니다.")
    @PostMapping("/{answerId}/reveal")
    public ApiResponse<Void> revealPartnerAnswer(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long answerId) {
        answerService.revealPartnerAnswer(userId, answerId);
        return ApiResponse.success("상대방의 답변이 공개되었습니다.", null);
    }
}