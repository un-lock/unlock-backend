package com.unlock.api.domain.auth.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.domain.auth.dto.AuthDto.EmailRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SocialLoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.TokenResponse;
import com.unlock.api.domain.auth.dto.AuthDto.VerifyRequest;
import com.unlock.api.domain.auth.service.AuthService;
import com.unlock.api.domain.auth.service.EmailService;
import com.unlock.api.domain.user.entity.AuthProvider;
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
    private final AuthService authService;

    /**
     * 인증번호 이메일 발송 요청
     */
    @PostMapping("/email/request")
    public ApiResponse<Void> requestEmailCode(@RequestBody @Valid EmailRequest request) {
        emailService.sendVerificationEmail(request.getEmail());
        return ApiResponse.success("인증번호가 발송되었습니다.", null);
    }

    /**
     * 인증번호 확인 요청
     */
    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(@RequestBody @Valid VerifyRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ApiResponse.success("인증에 성공하였습니다.", null);
    }

    /**
     * 카카오 로그인
     */
    @PostMapping("/kakao")
    public ApiResponse<TokenResponse> kakaoLogin(@RequestBody @Valid SocialLoginRequest request) {
        return ApiResponse.success("카카오 로그인 성공", authService.socialLogin(AuthProvider.KAKAO, request.getToken()));
    }

    /**
     * 구글 로그인
     */
    @PostMapping("/google")
    public ApiResponse<TokenResponse> googleLogin(@RequestBody @Valid SocialLoginRequest request) {
        return ApiResponse.success("구글 로그인 성공", authService.socialLogin(AuthProvider.GOOGLE, request.getToken()));
    }

    /**
     * 애플 로그인
     */
    @PostMapping("/apple")
    public ApiResponse<TokenResponse> appleLogin(@RequestBody @Valid SocialLoginRequest request) {
        return ApiResponse.success("애플 로그인 성공", authService.socialLogin(AuthProvider.APPLE, request.getToken()));
    }
}