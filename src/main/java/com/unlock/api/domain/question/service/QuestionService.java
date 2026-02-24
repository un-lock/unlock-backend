package com.unlock.api.domain.question.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.question.dto.QuestionDto.QuestionResponse;
import com.unlock.api.domain.question.entity.CoupleQuestion;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.question.repository.CoupleQuestionRepository;
import com.unlock.api.domain.question.repository.QuestionRepository;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

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
     * [리팩토링]: 배정 로직을 직접 수행하지 않고, 이미 배정된 질문이 있는지 조회만 수행합니다.
     */
    @Transactional(readOnly = true)
    public QuestionResponse getTodayQuestion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 1. 오늘 날짜로 배정된 질문이 있는지 확인
        LocalDate today = LocalDate.now();
        CoupleQuestion coupleQuestion = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND)); // 아직 질문이 배정되지 않은 경우

        Question question = coupleQuestion.getQuestion();
        boolean isAnswered = answerRepository.existsByUserAndQuestion(user, question);

        return convertToResponse(question, isAnswered);
    }

    /**
     * 커플에게 질문 배정 (오직 스케줄러 전용)
     * [리팩토링]: 기존 질문 이월(Carry-over) 및 신규 배정 로직을 한곳에서 관리합니다.
     */
    public Question assignQuestionToCouple(Couple couple) {
        LocalDate today = LocalDate.now();

        // 1. 이미 오늘 배정된 질문이 있다면 중복 배정 방지
        Optional<CoupleQuestion> todayRecord = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today);
        if (todayRecord.isPresent()) {
            return todayRecord.get().getQuestion();
        }

        // 2. [질문 이월 로직]: 마지막 질문이 미완료 상태라면 날짜만 오늘로 갱신
        Optional<CoupleQuestion> lastRecord = coupleQuestionRepository.findTopByCoupleOrderByAssignedDateDesc(couple);
        
        if (lastRecord.isPresent()) {
            CoupleQuestion last = lastRecord.get();
            boolean user1Finished = answerRepository.existsByUserAndQuestion(couple.getUser1(), last.getQuestion());
            boolean user2Finished = answerRepository.existsByUserAndQuestion(couple.getUser2(), last.getQuestion());

            if (!(user1Finished && user2Finished)) {
                log.info("[MOVE/CARRY-OVER] 커플(ID:{}) 미완료 질문 발견 -> 날짜를 {}로 이동", couple.getId(), today);
                last.updateAssignedDate(today);
                return last.getQuestion();
            }
        }

        // 3. [신규 배정]: 이월된 질문이 없다면 새로운 질문 랜덤 추출
        Question randomQuestion = questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        CoupleQuestion newAssignment = CoupleQuestion.builder()
                .couple(couple)
                .question(randomQuestion)
                .assignedDate(today)
                .build();
        
        coupleQuestionRepository.save(newAssignment);
        log.info("[NEW] 커플(ID:{}) 새로운 질문 배정 완료: ID={}", couple.getId(), randomQuestion.getId());
        return randomQuestion;
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