package com.unlock.api.domain.question.repository;

import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.question.entity.CoupleQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CoupleQuestionRepository extends JpaRepository<CoupleQuestion, Long> {
    
    /**
     * 특정 커플에게 특정 날짜에 배정된 질문 조회
     */
    Optional<CoupleQuestion> findByCoupleAndAssignedDate(Couple couple, LocalDate assignedDate);
}
