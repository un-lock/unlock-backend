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
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 에러가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메소드입니다."),
    
    // Auth (A)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증되지 않은 사용자입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "해당 리소스에 대한 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않거나 만료된 토큰입니다."),
    AUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "A004", "인증번호가 존재하지 않거나 만료되었습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "A005", "인증번호가 일치하지 않습니다."),
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "A006", "이메일 또는 비밀번호가 일치하지 않습니다."),
    
    // User (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U002", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U003", "이미 사용 중인 닉네임입니다."),
    
    // Couple (CP)
    COUPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "연결된 커플 정보를 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "CP002", "유효하지 않은 초대 코드입니다."),
    ALREADY_CONNECTED(HttpStatus.BAD_REQUEST, "CP003", "귀하는 이미 다른 커플과 연결되어 있습니다."),
    CANNOT_CONNECT_SELF(HttpStatus.BAD_REQUEST, "CP004", "자기 자신과는 커플을 맺을 수 없습니다."),
    PARTNER_ALREADY_CONNECTED(HttpStatus.BAD_REQUEST, "CP005", "상대방이 이미 다른 커플과 연결된 상태입니다."),
    PENDING_REQUEST_EXISTS(HttpStatus.BAD_REQUEST, "CP006", "상대방이 이미 다른 사람으로부터 연결 신청을 받은 상태입니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CP007", "처리할 연결 신청이 존재하지 않습니다."),
    
    // Question & Answer (Q)
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "해당 날짜의 질문을 찾을 수 없습니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "Q002", "해당 답변을 찾을 수 없습니다."),
    ANSWER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Q003", "이미 오늘의 답변을 등록하셨습니다."),
    PARTNER_ANSWER_LOCKED(HttpStatus.FORBIDDEN, "Q004", "본인의 답변을 먼저 등록해야 파트너의 답변을 볼 수 있습니다."),
    PARTNER_NOT_ANSWERED(HttpStatus.BAD_REQUEST, "Q005", "상대방이 아직 답변을 작성하지 않아 잠금을 해제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
