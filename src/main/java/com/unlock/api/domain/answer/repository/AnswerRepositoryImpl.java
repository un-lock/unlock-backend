package com.unlock.api.domain.answer.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.entity.QAnswer;
import com.unlock.api.domain.answer.entity.QAnswerReveal;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.question.entity.QCoupleQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AnswerRepositoryCustom의 구현체
 * 이름 규칙: [Repository명] + Impl (중요!)
 */
@Repository
@RequiredArgsConstructor
public class AnswerRepositoryImpl implements AnswerRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ArchiveSummaryResponse> findMonthlyArchiveSummary(Couple couple, Long userId, Long partnerId, boolean isSubscribed, int year, int month) {
        QCoupleQuestion coupleQuestion = QCoupleQuestion.coupleQuestion;
        QAnswer myAnswer = new QAnswer("myAnswer");
        QAnswer partnerAnswer = new QAnswer("partnerAnswer");
        QAnswerReveal answerReveal = new QAnswerReveal("answerReveal");

        BooleanExpression isRevealedExpr = isSubscribed
                ? Expressions.asBoolean(true)
                : answerReveal.id.isNotNull();

        return queryFactory
                .select(Projections.constructor(ArchiveSummaryResponse.class,
                        coupleQuestion.question.id,
                        coupleQuestion.question.content,
                        coupleQuestion.assignedDate,
                        myAnswer.id.isNotNull(),      // 내 답변 존재 여부
                        partnerAnswer.id.isNotNull(), // 파트너 답변 존재 여부
                        isRevealedExpr                // 열람 가능 여부
                ))
                .from(coupleQuestion)
                .leftJoin(myAnswer).on(
                        myAnswer.question.eq(coupleQuestion.question)
                        .and(myAnswer.user.id.eq(userId))
                )
                .leftJoin(partnerAnswer).on(
                        partnerAnswer.question.eq(coupleQuestion.question)
                        .and(partnerAnswer.user.id.eq(partnerId))
                )
                .leftJoin(answerReveal).on(
                        answerReveal.answer.eq(partnerAnswer)
                        .and(answerReveal.user.id.eq(userId))
                )
                .where(
                        coupleQuestion.couple.eq(couple)
                        .and(coupleQuestion.assignedDate.year().eq(year))
                        .and(coupleQuestion.assignedDate.month().eq(month))
                )
                .orderBy(coupleQuestion.assignedDate.asc())
                .fetch();
    }

    @Override
    public List<ArchiveSummaryResponse> findArchiveList(Couple couple, Long userId, Long partnerId, boolean isSubscribed, int page, int size, String sortDirection) {
        QCoupleQuestion coupleQuestion = QCoupleQuestion.coupleQuestion;
        QAnswer myAnswer = new QAnswer("myAnswer");
        QAnswer partnerAnswer = new QAnswer("partnerAnswer");
        QAnswerReveal answerReveal = new QAnswerReveal("answerReveal");

        BooleanExpression isRevealedExpr = isSubscribed
                ? Expressions.asBoolean(true)
                : answerReveal.id.isNotNull();

        return queryFactory
                .select(Projections.constructor(ArchiveSummaryResponse.class,
                        coupleQuestion.question.id,
                        coupleQuestion.question.content,
                        coupleQuestion.assignedDate,
                        myAnswer.id.isNotNull(),
                        partnerAnswer.id.isNotNull(),
                        isRevealedExpr
                ))
                .from(coupleQuestion)
                .leftJoin(myAnswer).on(
                        myAnswer.question.eq(coupleQuestion.question)
                                .and(myAnswer.user.id.eq(userId))
                )
                .leftJoin(partnerAnswer).on(
                        partnerAnswer.question.eq(coupleQuestion.question)
                                .and(partnerAnswer.user.id.eq(partnerId))
                )
                .leftJoin(answerReveal).on(
                        answerReveal.answer.eq(partnerAnswer)
                                .and(answerReveal.user.id.eq(userId))
                )
                .where(coupleQuestion.couple.eq(couple))
                .orderBy(sortDirection.equalsIgnoreCase("DESC") ?
                        coupleQuestion.assignedDate.desc() : coupleQuestion.assignedDate.asc())
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }
}
