package com.unlock.api.domain.auth.controller;

import com.unlock.api.common.dto.ApiCommonResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.auth.dto.AuthDto.EmailRequest;
import com.unlock.api.domain.auth.dto.AuthDto.LoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.LogoutRequest;
import com.unlock.api.domain.auth.dto.AuthDto.PasswordFindRequest;
import com.unlock.api.domain.auth.dto.AuthDto.PasswordResetRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SignupRequest;
import com.unlock.api.domain.auth.dto.AuthDto.SocialLoginRequest;
import com.unlock.api.domain.auth.dto.AuthDto.TokenResponse;
import com.unlock.api.domain.auth.dto.AuthDto.VerifyRequest;
import com.unlock.api.domain.auth.service.AuthService;
import com.unlock.api.domain.auth.service.AuthService.LoginDto;
import com.unlock.api.domain.auth.service.EmailService;
import com.unlock.api.domain.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 인증 및 계정 관리 API 컨트롤러
 */
@Tag(name = "1. Auth", description = "회원가입, 로그인 및 비밀번호 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailService emailService;
    private final AuthService authService;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final long REFRESH_TOKEN_MAX_AGE = 14 * 24 * 60 * 60; // 14일

    @Operation(summary = "이메일 인증번호 발송", description = "사용자 이메일로 6자리 인증번호를 전송합니다.")
    @ApiResponse(responseCode = "200", description = "발송 성공")
    @PostMapping("/email/request")
    public ApiCommonResponse<Void> requestEmailCode(@RequestBody @Valid EmailRequest request) {
        emailService.sendVerificationEmail(request.getEmail());
        return ApiCommonResponse.success("인증번호가 발송되었습니다.", null);
    }

    @Operation(summary = "이메일 인증번호 확인", description = "발송된 인증번호를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "인증 성공")
    @PostMapping("/email/verify")
    public ApiCommonResponse<Void> verifyEmailCode(@RequestBody @Valid VerifyRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ApiCommonResponse.success("인증에 성공하였습니다.", null);
    }

    @Operation(summary = "이메일 회원가입", description = "최종 회원가입을 수행합니다.")
    @ApiResponse(responseCode = "200", description = "가입 성공")
    @PostMapping("/signup")
    public ApiCommonResponse<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ApiCommonResponse.success("회원가입에 성공하였습니다.", null);
    }

    @Operation(summary = "이메일 로그인", description = "이메일 로그인 후 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @PostMapping("/login")
    public ApiCommonResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.login(request);
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiCommonResponse.success("로그인 성공", loginDto.toTokenResponse());
    }

    @Operation(summary = "비밀번호 찾기 인증번호 발송", description = "가입된 이메일로 비밀번호 재설정용 인증번호를 전송합니다.")
    @ApiResponse(responseCode = "200", description = "발송 성공")
    @PostMapping("/password/find")
    public ApiCommonResponse<Void> findPassword(@RequestBody @Valid PasswordFindRequest request) {
        authService.requestPasswordResetCode(request.getEmail());
        return ApiCommonResponse.success("비밀번호 재설정 인증번호가 발송되었습니다.", null);
    }

    @Operation(summary = "임시 비밀번호 발급", description = "인증번호 확인 후 새 임시 비밀번호를 이메일로 전송합니다.")
    @ApiResponse(responseCode = "200", description = "발급 성공")
    @PostMapping("/password/reset")
    public ApiCommonResponse<Void> resetPassword(@RequestBody @Valid PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiCommonResponse.success("임시 비밀번호가 이메일로 발송되었습니다.", null);
    }

    @Operation(summary = "토큰 재발급")
    @ApiResponse(responseCode = "200", description = "재발급 성공",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @PostMapping("/reissue")
    public ApiCommonResponse<TokenResponse> reissue(
            @Parameter(hidden = true) @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            HttpServletResponse response) {
        LoginDto loginDto = authService.reissue(refreshToken);
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiCommonResponse.success("토큰 재발급 성공", loginDto.toTokenResponse());
    }

    @Operation(summary = "로그아웃", description = "로그아웃 처리를 수행합니다. FCM 토큰을 함께 보내면 해당 기기의 알림이 해제됩니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ApiCommonResponse<Void> logout(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletResponse response) {
        authService.logout(userId, request != null ? request.getFcmToken() : null);
        deleteRefreshTokenCookie(response);
        return ApiCommonResponse.success("로그아웃 성공", null);
    }

    @Operation(summary = "카카오 로그인", description = "카카오 소셜 로그인을 수행합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @PostMapping("/kakao")
    public ApiCommonResponse<TokenResponse> kakaoLogin(@RequestBody @Valid SocialLoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.socialLogin(AuthProvider.KAKAO, request.getToken(), request.getFcmToken());
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ApiCommonResponse.success("카카오 로그인 성공", loginDto.toTokenResponse());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
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
