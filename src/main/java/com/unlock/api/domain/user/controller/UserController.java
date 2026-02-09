package com.unlock.api.domain.user.controller;

import com.unlock.api.common.dto.ApiResponse;
import com.unlock.api.common.security.annotation.CurrentUser;
import com.unlock.api.domain.user.dto.UserDto.NicknameUpdateRequest;
import com.unlock.api.domain.user.service.UserService;
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
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 닉네임 변경
     */
    @PatchMapping("/me/nickname")
    public ApiResponse<String> updateNickname(
            @CurrentUser Long userId,
            @RequestBody @Valid NicknameUpdateRequest request) {
        return ApiResponse.success("닉네임이 변경되었습니다.", userService.updateNickname(userId, request));
    }

    /**
     * 회원 탈퇴 (데이터 즉시 파기)
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@CurrentUser Long userId) {
        userService.withdraw(userId);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.", null);
    }
}
