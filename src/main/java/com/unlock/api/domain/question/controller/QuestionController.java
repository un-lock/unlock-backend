package com.unlock.api.domain.question.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.question.dto.QuestionDto.QuestionResponse;
import com.unlock.api.domain.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 질문 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 오늘의 질문 조회 (커플별 랜덤 배정)
     */
    @GetMapping("/today")
    public ApiResponse<QuestionResponse> getTodayQuestion(@CurrentUser Long userId) {
        return ApiResponse.success("오늘의 질문 조회 성공", questionService.getTodayQuestion(userId));
    }
}
