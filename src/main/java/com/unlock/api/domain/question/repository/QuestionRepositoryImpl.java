package com.unlock.api.domain.question.repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.unlock.api.domain.question.entity.QCoupleQuestion;
import com.unlock.api.domain.question.entity.QQuestion;
import com.unlock.api.domain.question.entity.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * QuestionRepositoryCustom의 구현체
 * Querydsl을 사용하여 복잡한 랜덤 추출 로직을 Type-safe하게 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class QuestionRepositoryImpl implements QuestionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Question> findRandomQuestionNotAssignedToCouple(Long coupleId) {
        QQuestion question = QQuestion.question;
        QCoupleQuestion coupleQuestion = QCoupleQuestion.coupleQuestion;

        // 1. 서브쿼리: 해당 커플에게 이미 배정된 적이 있는 모든 질문 ID 조회
        var assignedQuestionIds = JPAExpressions
                .select(coupleQuestion.question.id)
                .from(coupleQuestion)
                .where(coupleQuestion.couple.id.eq(coupleId));

        // 2. 메인쿼리: 배정되지 않은 질문들 중 랜덤으로 1개 추출
        // DB의 random() 함수를 호출하여 무작위 정렬 후 첫 번째 항목을 가져옵니다.
        Question result = queryFactory
                .selectFrom(question)
                .where(question.id.notIn(assignedQuestionIds))
                .orderBy(Expressions.numberTemplate(Double.class, "function('random')").asc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }
}
