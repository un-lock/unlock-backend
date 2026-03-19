package com.unlock.api.domain.question.repository;

import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.question.entity.QuestionCategory;
import java.util.List;
import java.util.Optional;

/**
 * Question 관련 커스텀 쿼리 인터페이스
 */
public interface QuestionRepositoryCustom {
    
    /**
     * 특정 커플이 아직 배정받지 않은 모든 질문들 중 랜덤하게 하나를 가져옵니다.
     */
    Optional<Question> findRandomQuestionNotAssignedToCouple(Long coupleId);

    /**
     * 특정 커플이 아직 배정받지 않은 질문들 중 지정된 카테고리에서 랜덤하게 하나를 가져옵니다.
     */
    Optional<Question> findRandomQuestionNotAssignedToCoupleByCategory(Long coupleId, QuestionCategory category);

    /**
     * 특정 커플이 아직 배정받지 않은 질문들 중 지정된 카테고리 목록에서 랜덤하게 하나를 가져옵니다.
     * (예: SPICY + HOT_SPICY 통합 풀)
     */
    Optional<Question> findRandomQuestionNotAssignedToCoupleByCategories(Long coupleId, List<QuestionCategory> categories);
}
