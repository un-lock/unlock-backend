package com.unlock.api.domain.user.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.user.dto.UserDto.NicknameUpdateRequest;
import com.unlock.api.domain.user.dto.UserDto.PasswordUpdateRequest;
import com.unlock.api.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 정보 관리 API 컨트롤러
 */
@Tag(name = "6. User", description = "내 정보 수정 및 회원 탈퇴 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 닉네임 변경", description = "닉네임을 새로운 이름으로 변경합니다. (중복 허용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공",
            content = @Content(schema = @Schema(implementation = String.class, example = "새로운연인")))
    @PatchMapping("/me/nickname")
    public ApiResponse<String> updateNickname(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestBody @Valid NicknameUpdateRequest request) {
        return ApiResponse.success("닉네임이 변경되었습니다.", userService.updateNickname(userId, request));
    }

    @Operation(summary = "비밀번호 변경", description = "로그인된 상태에서 현재 비밀번호를 확인한 후 새로운 비밀번호로 변경합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공")
    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestBody @Valid PasswordUpdateRequest request) {
        userService.updatePassword(userId, request);
        return ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.", null);
    }

    @Operation(summary = "회원 탈퇴 (데이터 즉시 파기)", description = "서비스 이용을 중단하고 모든 개인정보 및 대화 기록을 즉시 영구 파기합니다. ⚠️주의: 복구 불가")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 완료")
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@Parameter(hidden = true) @CurrentUser Long userId) {
        userService.withdraw(userId);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.", null);
    }
}