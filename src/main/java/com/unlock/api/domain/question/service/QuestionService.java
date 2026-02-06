package com.unlock.api.domain.question.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
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
    private final UserRepository userRepository;

    /**
     * 오늘의 질문 조회 (유저용 API)
     */
    public QuestionResponse getTodayQuestion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 현재 배정된 질문 조회 또는 즉시 배정
        Question question = assignQuestionToCouple(couple);

        return convertToResponse(question);
    }

    /**
     * 커플에게 질문 배정 (스케줄러 및 API 공용)
     * - 오늘 이미 배정된 질문이 있다면 반환
     * - 없다면 아직 받지 않은 질문 중 랜덤으로 하나 배정
     */
    public Question assignQuestionToCouple(Couple couple) {
        LocalDate today = LocalDate.now();

        return coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today)
                .map(CoupleQuestion::getQuestion)
                .orElseGet(() -> {
                    // 중복되지 않은 랜덤 질문 하나 추출
                    Question randomQuestion = questionRepository.findRandomQuestionNotAssignedToCouple(couple.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

                    // 배정 기록 저장
                    CoupleQuestion coupleQuestion = CoupleQuestion.builder()
                            .couple(couple)
                            .question(randomQuestion)
                            .assignedDate(today)
                            .build();
                    
                    coupleQuestionRepository.save(coupleQuestion);
                    log.info("커플(ID:{})에게 새로운 질문 배정 완료: {}", couple.getId(), randomQuestion.getContent());
                    return randomQuestion;
                });
    }

    private QuestionResponse convertToResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory().getDescription())
                .build();
    }
}