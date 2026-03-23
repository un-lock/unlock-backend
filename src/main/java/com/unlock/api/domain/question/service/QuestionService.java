package com.unlock.api.domain.question.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.question.dto.QuestionDto.QuestionResponse;
import com.unlock.api.domain.question.entity.CoupleQuestion;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.question.entity.QuestionCategory;
import com.unlock.api.domain.question.repository.CoupleQuestionRepository;
import com.unlock.api.domain.question.repository.QuestionRepository;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 질문 조회 및 랜덤 배정 로직 담당 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CoupleQuestionRepository coupleQuestionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    /**
     * 오늘의 질문 조회 (유저용 API)
     * [수정]: 데이터를 변경(이월)하지 않고, 가장 최근에 배정된 질문을 그대로 보여줍니다.
     */
    @Transactional(readOnly = true)
    public QuestionResponse getTodayQuestion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 1. 날짜와 상관없이 이 커플에게 배정된 가장 마지막 질문 조회
        CoupleQuestion lastRecord = coupleQuestionRepository.findTopByCoupleOrderByAssignedDateDesc(couple)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        Question question = lastRecord.getQuestion();
        boolean isAnswered = answerRepository.existsByUserAndQuestion(user, question);

        return convertToResponse(question, isAnswered);
    }

    /**
     * 커플에게 질문 배정 (스케줄러 전용)
     * 1. 이미 오늘 배정된 질문이 있다면 반환 (중복 방지)
     * 2. 마지막 질문이 미완료라면 오늘 날짜로 이월 (Carry-over) — 사이클 진행 없음
     * 3. 이월할 질문이 없다면 5일 사이클에 따라 새로운 질문 배정
     */
    public Question assignQuestionToCouple(Couple couple) {
        LocalDate today = LocalDate.now();

        // 1. 이미 오늘 배정된 질문이 있다면 중복 방지
        Optional<CoupleQuestion> todayRecord = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today);
        if (todayRecord.isPresent()) {
            return todayRecord.get().getQuestion();
        }

        // 2. [질문 이월 로직]: 마지막 질문이 미완료 상태라면 날짜만 오늘로 갱신 (사이클 유지)
        Optional<CoupleQuestion> lastRecordOpt = coupleQuestionRepository.findTopByCoupleOrderByAssignedDateDesc(couple);

        if (lastRecordOpt.isPresent()) {
            CoupleQuestion last = lastRecordOpt.get();
            boolean user1Finished = answerRepository.existsByUserAndQuestion(couple.getUser1(), last.getQuestion());
            boolean user2Finished = answerRepository.existsByUserAndQuestion(couple.getUser2(), last.getQuestion());

            if (!(user1Finished && user2Finished)) {
                log.debug("[SCHEDULE] 커플(ID:{}) 미완료 질문 발견 -> 날짜를 {}로 이월", couple.getId(), today);
                last.updateAssignedDate(today);
                return last.getQuestion();
            }
        }

        // 3. [신규 배정]: 5일 사이클 기반으로 카테고리 결정 후 질문 추출
        return assignNextCycleQuestion(couple, today);
    }

    /**
     * 커플 첫 생성 시 호출 — 첫 질문은 무조건 SPICY, 이후 사이클 초기화
     * cycleDay=1로 설정하여 다음 스케줄러 호출부터 사이클 2일차부터 진행
     */
    public Question assignFirstQuestionToCouple(Couple couple) {
        // 첫 질문 무조건 SPICY
        Question firstQuestion = questionRepository.findRandomQuestionNotAssignedToCoupleByCategory(couple.getId(), QuestionCategory.SPICY)
                .orElseGet(() -> questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND)));

        CoupleQuestion newAssignment = CoupleQuestion.builder()
                .couple(couple)
                .question(firstQuestion)
                .assignedDate(LocalDate.now())
                .build();
        coupleQuestionRepository.save(newAssignment);

        // 사이클 초기화: cycleDay=1로 설정, sweetDay는 2~5 랜덤 (2일차부터 사이클 적용)
        int sweetDay = ThreadLocalRandom.current().nextInt(2, 6);
        couple.initCycle(sweetDay);
        couple.advanceCycle(1, sweetDay);

        log.debug("[COUPLE_CREATED] 커플(ID:{}) 첫 SPICY 질문 배정 완료 — questionId={}, sweetDay={}",
                couple.getId(), firstQuestion.getId(), sweetDay);
        return firstQuestion;
    }

    /**
     * 5일 사이클에 따라 카테고리를 결정하고 질문을 배정합니다.
     * - cycleDay가 sweetDay와 일치하면 SWEET
     * - 나머지 4일은 SPICY (isHotSpicyEnabled=true면 SPICY+HOT_SPICY 통합 풀)
     * - 사이클 day 5 완료 시 다음 사이클의 sweetDay 재추첨
     */
    private Question assignNextCycleQuestion(Couple couple, LocalDate date) {
        int nextDay = (couple.getQuestionCycleDay() % 5) + 1; // 0→1, 1→2, ..., 5→1
        boolean isSweet = nextDay == couple.getQuestionSweetDay();

        Question question = pickQuestion(couple, isSweet);

        CoupleQuestion newAssignment = CoupleQuestion.builder()
                .couple(couple)
                .question(question)
                .assignedDate(date)
                .build();
        coupleQuestionRepository.save(newAssignment);

        int currentSweetDay = couple.getQuestionSweetDay();
        int newSweetDay = (nextDay == 5) ? ThreadLocalRandom.current().nextInt(1, 6) : currentSweetDay;
        couple.advanceCycle(nextDay, newSweetDay);

        log.debug("[SCHEDULE] 커플(ID:{}) 사이클 day={}/{} 질문 배정 — category={}, questionId={}",
                couple.getId(), nextDay, currentSweetDay, question.getCategory(), question.getId());
        return question;
    }

    /**
     * isSweet 여부와 isHotSpicyEnabled에 따라 적절한 카테고리 풀에서 질문을 추출합니다.
     */
    private Question pickQuestion(Couple couple, boolean isSweet) {
        if (isSweet) {
            return questionRepository.findRandomQuestionNotAssignedToCoupleByCategory(couple.getId(), QuestionCategory.SWEET)
                    .orElseGet(() -> questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND)));
        }
        if (couple.isHotSpicyEnabled()) {
            return questionRepository.findRandomQuestionNotAssignedToCoupleByCategories(
                            couple.getId(), List.of(QuestionCategory.SPICY, QuestionCategory.HOT_SPICY))
                    .orElseGet(() -> questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND)));
        }
        return questionRepository.findRandomQuestionNotAssignedToCoupleByCategory(couple.getId(), QuestionCategory.SPICY)
                .orElseGet(() -> questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND)));
    }

    /**
     * Entity -> DTO 변환
     */
    private QuestionResponse convertToResponse(Question question, boolean isAnswered) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory())
                .isAnswered(isAnswered)
                .build();
    }
}