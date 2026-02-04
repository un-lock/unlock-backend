package com.unlock.api.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전역 공통 응답 규격 클래스
 * 모든 API 응답은 이 클래스를 거쳐 동일한 포맷으로 반환됩니다.
 *
 * @param <T> 응답 데이터의 타입
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success; // 요청 성공 여부
    private String code;     // 내부 에러 코드 (성공 시 "COMMON_000")
    private String message;  // 응답 메시지 (성공 시 "성공", 실패 시 에러 메시지)
    private T data;          // 실제 응답 데이터

    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "COMMON_000", "성공", data);
    }

    /**
     * 성공 응답 생성 (메시지와 데이터 포함)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "COMMON_000", message, data);
    }

    /**
     * 에러 응답 생성
     */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
