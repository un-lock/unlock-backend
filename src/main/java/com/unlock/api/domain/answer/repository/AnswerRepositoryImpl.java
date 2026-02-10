package com.unlock.api.domain.answer.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.entity.QAnswer;
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
    public List<ArchiveSummaryResponse> findMonthlyArchiveSummary(Couple couple, Long userId, Long partnerId, int year, int month) {
        QCoupleQuestion coupleQuestion = QCoupleQuestion.coupleQuestion;
        QAnswer myAnswer = new QAnswer("myAnswer");
        QAnswer partnerAnswer = new QAnswer("partnerAnswer");

        return queryFactory
                .select(Projections.constructor(ArchiveSummaryResponse.class,
                        coupleQuestion.question.id,
                        coupleQuestion.question.content,
                        coupleQuestion.assignedDate,
                        myAnswer.id.isNotNull(),      // 내 답변 존재 여부
                        partnerAnswer.id.isNotNull()  // 파트너 답변 존재 여부
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
                .where(
                        coupleQuestion.couple.eq(couple)
                        .and(coupleQuestion.assignedDate.year().eq(year))
                        .and(coupleQuestion.assignedDate.month().eq(month))
                )
                .orderBy(coupleQuestion.assignedDate.asc())
                .fetch();
    }
}
