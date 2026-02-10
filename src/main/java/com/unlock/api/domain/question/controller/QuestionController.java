package com.unlock.api.domain.question.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.question.dto.QuestionDto.QuestionResponse;
import com.unlock.api.domain.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 질문 관련 API 컨트롤러
 */
@Tag(name = "3. Question", description = "오늘의 질문 조회 및 배정 API")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "오늘의 질문 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = QuestionResponse.class)))
    @GetMapping("/today")
    public ApiResponse<QuestionResponse> getTodayQuestion(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("오늘의 질문 조회 성공", questionService.getTodayQuestion(userId));
    }
}
