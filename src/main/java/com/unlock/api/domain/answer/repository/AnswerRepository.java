package com.unlock.api.domain.answer.repository;

import com.unlock.api.domain.answer.entity.Answer;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    /**
     * 특정 유저가 특정 질문에 대해 작성한 답변 조회
     */
    Optional<Answer> findByUserAndQuestion(User user, Question question);
    
    /**
     * 특정 유저가 특정 질문에 답변을 남겼는지 확인
     */
    boolean existsByUserAndQuestion(User user, Question question);

    /**
     * 특정 유저의 모든 답변 삭제 (커플 해제 시 사용)
     */
    void deleteAllByUser(User user);

    /**
     * 여러 질문들에 대한 특정 유저의 답변 리스트 일괄 조회
     */
    @Query("SELECT a FROM Answer a WHERE a.user = :user AND a.question.id IN :questionIds")
    List<Answer> findAllByUserAndQuestionIds(@Param("user") User user, @Param("questionIds") List<Long> questionIds);
}
