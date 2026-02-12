package com.unlock.api.domain.couple.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.couple.dto.CoupleDto.ConnectRequest;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleRequestResponse;
import com.unlock.api.domain.couple.dto.CoupleDto.CoupleResponse;
import com.unlock.api.domain.couple.service.CoupleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커플 연결 및 관리 관련 API 컨트롤러
 */
@Tag(name = "2. Couple", description = "커플 매칭, 연결 신청 및 해제 API")
@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @Operation(summary = "내 커플 정보 및 초대 코드 조회", description = "자신의 상태를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CoupleResponse.class)))
    @GetMapping("/me")
    public ApiResponse<CoupleResponse> getMyCoupleInfo(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("커플 정보 조회 성공", coupleService.getCoupleInfo(userId));
    }

    @Operation(summary = "커플 연결 신청 (초대 코드 입력)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신청 완료")
    @PostMapping("/request")
    public ApiResponse<Void> requestConnection(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestBody @Valid ConnectRequest request) {
        coupleService.requestConnection(userId, request.getInviteCode());
        return ApiResponse.success("커플 연결 신청을 보냈습니다.", null);
    }

    @Operation(summary = "나에게 온 연결 신청 확인")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CoupleRequestResponse.class)))
    @GetMapping("/requests")
    public ApiResponse<CoupleRequestResponse> getReceivedRequest(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("받은 신청 조회 성공", coupleService.getReceivedRequest(userId));
    }

    @Operation(summary = "연결 신청 수락")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "연결 성공")
    @PostMapping("/accept")
    public ApiResponse<Void> acceptConnection(@Parameter(hidden = true) @CurrentUser Long userId) {
        coupleService.acceptConnection(userId);
        return ApiResponse.success("커플 연결이 완료되었습니다.", null);
    }

    @Operation(summary = "연결 신청 거절")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공")
    @PostMapping("/reject")
    public ApiResponse<Void> rejectConnection(@Parameter(hidden = true) @CurrentUser Long userId) {
        coupleService.rejectConnection(userId);
        return ApiResponse.success("연결 신청을 거절했습니다.", null);
    }

    @Operation(summary = "커플 연결 해제 (데이터 즉시 파기)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공")
    @DeleteMapping
    public ApiResponse<Void> breakup(@Parameter(hidden = true) @CurrentUser Long userId) {
        coupleService.breakup(userId);
        return ApiResponse.success("커플 연결이 해제되었으며 모든 데이터가 파기되었습니다.", null);
    }
}
