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

    // Common (C)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 에러가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 메소드입니다."),
    
    // Auth (A)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증되지 않은 사용자입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),
    
    // User (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    
    // Couple (CP)
    COUPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "연결된 커플 정보를 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "CP002", "유효하지 않은 초대 코드입니다."),
    ALREADY_CONNECTED(HttpStatus.BAD_REQUEST, "CP003", "이미 커플이 연결되어 있습니다."),
    CANNOT_CONNECT_SELF(HttpStatus.BAD_REQUEST, "CP004", "자기 자신과는 연결할 수 없습니다."),
    PARTNER_ALREADY_CONNECTED(HttpStatus.BAD_REQUEST, "CP005", "상대방이 이미 다른 커플과 연결되어 있습니다."),
    
    // Question & Answer (Q)
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "질문을 찾을 수 없습니다."),
    ANSWER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Q002", "이미 오늘의 답변을 작성했습니다."),
    PARTNER_ANSWER_LOCKED(HttpStatus.FORBIDDEN, "Q003", "본인의 답변을 먼저 작성해야 파트너의 답변을 볼 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
