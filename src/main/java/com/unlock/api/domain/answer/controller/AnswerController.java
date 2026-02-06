package com.unlock.api.domain.answer.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.answer.dto.AnswerDto.AnswerRequest;
import com.unlock.api.domain.answer.dto.AnswerDto.TodayAnswerResponse;
import com.unlock.api.domain.answer.service.AnswerService;
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
@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    /**
     * 오늘의 답변 등록
     */
    @PostMapping
    public ApiResponse<Void> submitAnswer(
            @CurrentUser Long userId,
            @RequestBody @Valid AnswerRequest request) {
        answerService.submitAnswer(userId, request);
        return ApiResponse.success("답변이 등록되었습니다.", null);
    }

    /**
     * 오늘의 답변 현황 조회 (나와 파트너)
     */
    @GetMapping("/today")
    public ApiResponse<TodayAnswerResponse> getTodayAnswers(@CurrentUser Long userId) {
        return ApiResponse.success("오늘의 답변 조회 성공", answerService.getTodayAnswers(userId));
    }

    /**
     * 파트너 답변 잠금 해제 (광고 시청 완료 시 호출)
     */
    @PostMapping("/{answerId}/reveal")
    public ApiResponse<Void> revealPartnerAnswer(
            @CurrentUser Long userId,
            @PathVariable Long answerId) {
        answerService.revealPartnerAnswer(userId, answerId);
        return ApiResponse.success("상대방의 답변이 공개되었습니다.", null);
    }
}
