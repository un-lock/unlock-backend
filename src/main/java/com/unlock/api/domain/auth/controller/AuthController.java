package com.unlock.api.domain.auth.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.domain.auth.dto.AuthDto;
import com.unlock.api.domain.auth.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailService emailService;

    /**
     * 인증번호 이메일 발송 요청
     */
    @PostMapping("/email/request")
    public ApiResponse<Void> requestEmailCode(@RequestBody @Valid AuthDto.EmailRequest request) {
        emailService.sendVerificationEmail(request.getEmail());
        return ApiResponse.success("인증번호가 발송되었습니다.", null);
    }

    /**
     * 인증번호 확인 요청
     */
    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(@RequestBody @Valid AuthDto.VerifyRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ApiResponse.success("인증에 성공하였습니다.", null);
    }
}
