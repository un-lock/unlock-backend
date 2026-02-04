package com.unlock.api.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 정의
 * 비즈니스 로직에서 발생하는 예외 상황을 체계적으로 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (공통 에러: C로 시작)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 메소드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 에러가 발생했습니다."),
    
    // User & Couple (사용자/커플 관련: U, CP로 시작)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    COUPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "연결된 커플 정보를 찾을 수 없습니다."),
    
    // Question & Answer (질문/답변 관련: Q, A로 시작)
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "질문을 찾을 수 없습니다."),
    ANSWER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "A001", "이미 오늘의 답변을 작성했습니다."),
    PARTNER_ANSWER_LOCKED(HttpStatus.FORBIDDEN, "A002", "본인의 답변을 먼저 작성해야 파트너의 답변을 볼 수 있습니다.");

    private final HttpStatus status; // HTTP 상태 코드
    private final String code;       // 내부 식별 코드
    private final String message;    // 사용자에게 노출할 기본 에러 메시지
}