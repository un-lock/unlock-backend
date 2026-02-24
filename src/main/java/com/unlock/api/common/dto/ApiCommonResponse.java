package com.unlock.api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전역 공통 응답 규격 클래스
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "전역 공통 응답 규격")
public class ApiCommonResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "내부 에러/상태 코드", example = "COMMON_000")
    private String code;

    @Schema(description = "응답 메시지", example = "성공")
    private String message;

    @Schema(description = "실제 응답 데이터")
    private T data;

    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ApiCommonResponse<T> success(T data) {
        return new ApiCommonResponse<>(true, "COMMON_000", "성공", data);
    }

    /**
     * 성공 응답 생성 (메시지와 데이터 포함)
     */
    public static <T> ApiCommonResponse<T> success(String message, T data) {
        return new ApiCommonResponse<>(true, "COMMON_000", message, data);
    }

    /**
     * 에러 응답 생성
     */
    public static ApiCommonResponse<Void> error(String code, String message) {
        return new ApiCommonResponse<>(false, code, message, null);
    }
}