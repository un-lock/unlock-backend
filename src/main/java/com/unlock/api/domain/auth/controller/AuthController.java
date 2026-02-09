package com.unlock.api.domain.auth.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.auth.dto.AuthDto.EmailRequest;
import com.unlock.api.domain.auth.dto.AuthDto.LoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.ReissueRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SignupRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SocialLoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.TokenResponse;
import com.unlock.api.domain.auth.dto.AuthDto.VerifyRequest;
import com.unlock.api.domain.auth.service.AuthService;
import com.unlock.api.domain.auth.service.AuthService.LoginDto;
import com.unlock.api.domain.auth.service.EmailService;
import com.unlock.api.domain.user.entity.AuthProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
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

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final long REFRESH_TOKEN_MAX_AGE = 14 * 24 * 60 * 60; // 14일

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
     * 이메일 회원가입
     */
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ApiResponse.success("회원가입에 성공하였습니다.", null);
    }

    /**
     * 이메일 로그인
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.login(request);
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiResponse.success("로그인 성공", loginDto.toTokenResponse());
    }

    /**
     * 토큰 재발급
     */
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            HttpServletResponse response) {
        LoginDto loginDto = authService.reissue(refreshToken);
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiResponse.success("토큰 재발급 성공", loginDto.toTokenResponse());
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser Long userId, HttpServletResponse response) {
        authService.logout(userId);
        deleteRefreshTokenCookie(response);
        return ApiResponse.success("로그아웃 성공", null);
    }

    /**
     * 카카오 로그인
     */
    @PostMapping("/kakao")
    public ApiResponse<TokenResponse> kakaoLogin(@RequestBody @Valid SocialLoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.socialLogin(AuthProvider.KAKAO, request.getToken());
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiResponse.success("카카오 로그인 성공", loginDto.toTokenResponse());
    }

    /**
     * 구글 로그인
     */
    @PostMapping("/google")
    public ApiResponse<TokenResponse> googleLogin(@RequestBody @Valid SocialLoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.socialLogin(AuthProvider.GOOGLE, request.getToken());
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiResponse.success("구글 로그인 성공", loginDto.toTokenResponse());
    }

    /**
     * 애플 로그인
     */
    @PostMapping("/apple")
    public ApiResponse<TokenResponse> appleLogin(@RequestBody @Valid SocialLoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.socialLogin(AuthProvider.APPLE, request.getToken());
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiResponse.success("애플 로그인 성공", loginDto.toTokenResponse());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS 환경에서 사용 (배포 시 필수)
        cookie.setPath("/");
        cookie.setMaxAge((int) REFRESH_TOKEN_MAX_AGE);
        response.addCookie(cookie);
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
