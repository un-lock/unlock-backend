package com.unlock.api.domain.question.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 질문의 성격과 수위를 구분하는 카테고리 Enum
 */
@Getter
@RequiredArgsConstructor
public enum QuestionCategory {
    SWEET("일상"),
    SPICY("매운맛"),
    HOT_SPICY("제일 매운맛");

    private final String description;
}