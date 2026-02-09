package com.unlock.api.domain.question.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionCategory {
    ROMANCE("로맨틱"),
    DAILY("일상"),
    SPICY("은밀한"),
    DEEP_TALK("가치관");

    private final String description;
}
