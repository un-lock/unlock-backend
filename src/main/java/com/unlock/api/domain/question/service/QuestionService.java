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
     */
    public QuestionResponse getTodayQuestion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 현재 배정된 질문 조회 및 날짜 보정 로직 실행
        Question question = assignQuestionToCouple(couple);

        boolean isAnswered = answerRepository.existsByUserAndQuestion(user, question);

        return convertToResponse(question, isAnswered);
    }

    /**
     * 커플에게 질문 배정 (스케줄러 및 API 공용)
     * - [핵심 정책] 이전 질문 미완료 시 오늘 날짜로 이동
     * - 모두 완료 시에만 새로운 질문 배정
     */
    public Question assignQuestionToCouple(Couple couple) {
        LocalDate today = LocalDate.now();

        // 1. 이미 오늘 날짜로 배정된 질문이 있다면 그대로 반환
        Optional<CoupleQuestion> todayRecord = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today);
        if (todayRecord.isPresent()) {
            return todayRecord.get().getQuestion();
        }

        // 2. 가장 최근에 배정된 질문 조회
        Optional<CoupleQuestion> lastRecord = coupleQuestionRepository.findTopByCoupleOrderByAssignedDateDesc(couple);
        
        if (lastRecord.isPresent()) {
            CoupleQuestion last = lastRecord.get();
            boolean user1Finished = answerRepository.existsByUserAndQuestion(couple.getUser1(), last.getQuestion());
            boolean user2Finished = answerRepository.existsByUserAndQuestion(couple.getUser2(), last.getQuestion());

            // 한 명이라도 답변을 안 했다면? -> 해당 질문을 오늘 날짜로 '이사' 시킴
            if (!(user1Finished && user2Finished)) {
                log.info("커플(ID:{}) 미완료 질문 발견. 날짜를 {}에서 {}로 이동합니다.", couple.getId(), last.getAssignedDate(), today);
                last.updateAssignedDate(today);
                return last.getQuestion();
            }
        }

        // 3. 이전 질문이 없거나 모두 완료했다면 새로운 랜덤 질문 배정
        Question randomQuestion = questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        CoupleQuestion newAssignment = CoupleQuestion.builder()
                .couple(couple)
                .question(randomQuestion)
                .assignedDate(today)
                .build();
        
        coupleQuestionRepository.save(newAssignment);
        log.info("커플(ID:{})에게 새로운 질문 배정 완료: {}", couple.getId(), randomQuestion.getContent());
        return randomQuestion;
    }

    private QuestionResponse convertToResponse(Question question, boolean isAnswered) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory().getDescription())
                .isAnswered(isAnswered)
                .build();
    }
}