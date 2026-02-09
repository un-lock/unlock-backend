package com.unlock.api.domain.couple.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.couple.dto.CoupleDto.ConnectRequest;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleRequestResponse;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleResponse;
import com.unlock.api.domain.couple.service.CoupleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커플 연결 및 관리 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    /**
     * 내 커플 정보 및 초대 코드 조회
     */
    @GetMapping("/me")
    public ApiResponse<CoupleResponse> getMyCoupleInfo(@CurrentUser Long userId) {
        return ApiResponse.success("커플 정보 조회 성공", coupleService.getCoupleInfo(userId));
    }

    /**
     * 커플 연결 신청 (초대 코드 입력)
     */
    @PostMapping("/request")
    public ApiResponse<Void> requestConnection(
            @CurrentUser Long userId,
            @RequestBody @Valid ConnectRequest request) {
        coupleService.requestConnection(userId, request.getInviteCode());
        return ApiResponse.success("커플 연결 신청을 보냈습니다.", null);
    }

    /**
     * 나에게 온 연결 신청 확인
     */
    @GetMapping("/requests")
    public ApiResponse<CoupleRequestResponse> getReceivedRequest(@CurrentUser Long userId) {
        return ApiResponse.success("받은 신청 조회 성공", coupleService.getReceivedRequest(userId));
    }

    /**
     * 연결 신청 수락
     */
    @PostMapping("/accept")
    public ApiResponse<Void> acceptConnection(@CurrentUser Long userId) {
        coupleService.acceptConnection(userId);
        return ApiResponse.success("커플 연결이 완료되었습니다.", null);
    }

    /**
     * 연결 신청 거절
     */
    @PostMapping("/reject")
    public ApiResponse<Void> rejectConnection(@CurrentUser Long userId) {
        coupleService.rejectConnection(userId);
        return ApiResponse.success("연결 신청을 거절했습니다.", null);
    }
}