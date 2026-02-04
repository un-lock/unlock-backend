package com.unlock.api.domain.couple.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.domain.couple.dto.CoupleDto.ConnectRequest;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleResponse;
import com.unlock.api.domain.couple.service.CoupleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    /**
     * 내 커플 정보 및 초대 코드 조회
     * TODO: SecurityContext에서 userId를 가져오도록 수정 필요
     */
    @GetMapping("/me")
    public ApiResponse<CoupleResponse> getMyCoupleInfo(@RequestHeader("X-USER-ID") Long userId) {
        return ApiResponse.success("커플 정보 조회 성공", coupleService.getCoupleInfo(userId));
    }

    /**
     * 초대 코드로 커플 연결
     * TODO: SecurityContext에서 userId를 가져오도록 수정 필요
     */
    @PostMapping("/connect")
    public ApiResponse<Void> connectCouple(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody @Valid ConnectRequest request) {
        coupleService.connect(userId, request.getInviteCode());
        return ApiResponse.success("커플 연결 성공", null);
    }
}