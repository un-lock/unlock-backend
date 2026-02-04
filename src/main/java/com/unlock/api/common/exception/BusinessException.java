package com.unlock.api.common.exception;

import lombok.Getter;

/**
 * 애플리케이션의 비즈니스 로직 중 발생하는 예외를 처리하기 위한 클래스
 * ErrorCode를 필드로 가졌으며, 발생 시 GlobalExceptionHandler에서 가로채 처리합니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}