package com.unlock.api.domain.auth.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림의 종류를 정의하는 Enum
 * 프론트엔드(앱)에서 이 값을 읽어 알림 클릭 시 화면 이동을 결정합니다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    DAILY_QUESTION("오늘의 질문 도착"),
    PARTNER_ANSWER("상대방이 답변 등록 했을때"),
    COUPLE_REQUEST("커플 연결 신청"),
    COUPLE_CONNECTED("커플 연결 완료"),
    COUPLE_REQUEST_REJECTED("커플 연결 거절"),
    COUPLE_DISCONNECTED("커플 연결 해제");

    private final String description;
}