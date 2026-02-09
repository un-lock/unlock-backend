package com.unlock.api.domain.answer.repository;

import com.unlock.api.domain.answer.entity.Answer;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    /**
     * 특정 유저가 특정 질문에 대해 작성한 답변 조회
     */
    Optional<Answer> findByUserAndQuestion(User user, Question question);
    
    /**
     * 특정 유저가 특정 질문에 답변을 남겼는지 확인
     */
    boolean existsByUserAndQuestion(User user, Question question);
}
