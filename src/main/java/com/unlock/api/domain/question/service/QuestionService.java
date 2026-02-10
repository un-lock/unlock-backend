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

        Question question = assignQuestionToCouple(couple);
        boolean isAnswered = answerRepository.existsByUserAndQuestion(user, question);

        return convertToResponse(question, isAnswered);
    }

    /**
     * 커플에게 질문 배정 (스케줄러 및 API 공용)
     */
    public Question assignQuestionToCouple(Couple couple) {
        LocalDate today = LocalDate.now();

        Optional<CoupleQuestion> todayRecord = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today);
        if (todayRecord.isPresent()) {
            return todayRecord.get().getQuestion();
        }

        Optional<CoupleQuestion> lastRecord = coupleQuestionRepository.findTopByCoupleOrderByAssignedDateDesc(couple);
        
        if (lastRecord.isPresent()) {
            CoupleQuestion last = lastRecord.get();
            boolean user1Finished = answerRepository.existsByUserAndQuestion(couple.getUser1(), last.getQuestion());
            boolean user2Finished = answerRepository.existsByUserAndQuestion(couple.getUser2(), last.getQuestion());

            if (!(user1Finished && user2Finished)) {
                log.info("[MOVE] 커플(ID:{}) 미완료 질문 발견 -> 날짜를 {}로 이동", couple.getId(), today);
                last.updateAssignedDate(today);
                return last.getQuestion();
            }
        }

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
     * Entity -> DTO 변환 (Enum 타입 그대로 반환)
     */
    private QuestionResponse convertToResponse(Question question, boolean isAnswered) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory()) // .getDescription() 제거
                .isAnswered(isAnswered)
                .build();
    }
}