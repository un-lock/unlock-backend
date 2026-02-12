package com.unlock.api.domain.question.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 질문의 성격과 수위를 구분하는 카테고리 Enum
 */
@Getter
@RequiredArgsConstructor
public enum QuestionCategory {
    DAILY("일상"),
    ROMANCE("로맨틱"),
    SPICY("은밀한"),
    DEEP_TALK("가치관");

    private final String description;
}