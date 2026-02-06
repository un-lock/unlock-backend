package com.unlock.api.domain.question.repository;

import com.unlock.api.domain.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    /**
     * 특정 커플이 아직 배정받지 않은 모든 질문들 중 랜덤하게 하나를 가져옵니다.
     */
    @Query(value = "SELECT * FROM questions q WHERE q.id NOT IN " +
                   "(SELECT cq.question_id FROM couple_questions cq WHERE cq.couple_id = :coupleId) " +
                   "ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestionNotAssignedToCouple(@Param("coupleId") Long coupleId);
}