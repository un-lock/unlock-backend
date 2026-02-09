package com.unlock.api.domain.answer.repository;

import com.unlock.api.domain.answer.entity.Answer;
import com.unlock.api.domain.answer.entity.AnswerReveal;
import com.unlock.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRevealRepository extends JpaRepository<AnswerReveal, Long> {
    
    /**
     * 특정 유저가 특정 답변을 열람(광고 시청 등)했는지 확인
     */
    boolean existsByUserAndAnswer(User user, Answer answer);

    /**
     * 특정 유저의 모든 열람 기록 삭제 (커플 해제 시 사용)
     */
    void deleteAllByUser(User user);
}
