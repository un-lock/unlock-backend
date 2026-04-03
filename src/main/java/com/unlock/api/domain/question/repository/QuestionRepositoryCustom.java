package com.unlock.api.domain.question.repository;

import com.unlock.api.domain.question.entity.Question;
import java.util.Optional;

/**
 * Question 관련 커스텀 쿼리 인터페이스
 */
public interface QuestionRepositoryCustom {

    /**
     * 특정 커플이 아직 배정받지 않은 질문들 중 랜덤하게 하나를 가져옵니다.
     * isHotSpicyEnabled가 false이면 HOT_SPICY 카테고리를 제외합니다.
     */
    Optional<Question> findRandomQuestionNotAssignedToCouple(Long coupleId, boolean isHotSpicyEnabled);
}
