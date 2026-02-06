package com.unlock.api.domain.answer.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.dto.AnswerDto.AnswerRequest;
import com.unlock.api.domain.answer.dto.AnswerDto.MyAnswerDto;
import com.unlock.api.domain.answer.dto.AnswerDto.PartnerAnswerDto;
import com.unlock.api.domain.answer.dto.AnswerDto.TodayAnswerResponse;
import com.unlock.api.domain.answer.entity.Answer;
import com.unlock.api.domain.answer.entity.AnswerReveal;
import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.answer.repository.AnswerRevealRepository;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.question.entity.CoupleQuestion;
import com.unlock.api.domain.question.repository.CoupleQuestionRepository;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 답변 등록 및 열람 권한 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final AnswerRevealRepository answerRevealRepository;
    private final CoupleQuestionRepository coupleQuestionRepository;
    private final UserRepository userRepository;

    /**
     * 오늘의 답변 등록
     */
    public void submitAnswer(Long userId, AnswerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 오늘 우리 커플에게 배정된 질문 조회
        CoupleQuestion coupleQuestion = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 이미 답변했는지 확인
        if (answerRepository.existsByUserAndQuestion(user, coupleQuestion.getQuestion())) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        Answer answer = Answer.builder()
                .user(user)
                .question(coupleQuestion.getQuestion())
                .content(request.getContent())
                .build();

        answerRepository.save(answer);
    }

    /**
     * 오늘의 답변 현황 조회 (나와 파트너)
     */
    @Transactional(readOnly = true)
    public TodayAnswerResponse getTodayAnswers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        CoupleQuestion coupleQuestion = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 1. 내 답변 조회
        Answer myAnswer = answerRepository.findByUserAndQuestion(user, coupleQuestion.getQuestion())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_ANSWER_LOCKED));

        // 2. 파트너 답변 조회
        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, coupleQuestion.getQuestion()).orElse(null);

        // 3. 열람 권한 체크 (구독 중이거나, 광고를 봤거나)
        boolean isRevealed = false;
        if (partnerAnswer != null) {
            isRevealed = couple.isSubscribed() || answerRevealRepository.existsByUserAndAnswer(user, partnerAnswer);
        }

        return TodayAnswerResponse.builder()
                .myAnswer(convertToMyAnswerDto(myAnswer))
                .partnerAnswer(convertToPartnerAnswerDto(partner, partnerAnswer, isRevealed))
                .build();
    }

    /**
     * 파트너 답변 잠금 해제 (광고 시청 완료 시 호출)
     */
    public void revealPartnerAnswer(Long userId, Long answerId) {
        User user = userRepository.findById(userId).get();
        Answer partnerAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 이미 열려있는지 확인 후 저장
        if (!answerRevealRepository.existsByUserAndAnswer(user, partnerAnswer)) {
            answerRevealRepository.save(AnswerReveal.builder()
                    .user(user)
                    .answer(partnerAnswer)
                    .build());
        }
    }

    private MyAnswerDto convertToMyAnswerDto(Answer answer) {
        return MyAnswerDto.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }

    private PartnerAnswerDto convertToPartnerAnswerDto(User partner, Answer answer, boolean isRevealed) {
        if (answer == null) {
            return PartnerAnswerDto.builder()
                    .nickname(partner.getNickname())
                    .isWritten(false)
                    .build();
        }
        return PartnerAnswerDto.builder()
                .id(answer.getId())
                .nickname(partner.getNickname())
                .isWritten(true)
                .isRevealed(isRevealed)
                .content(isRevealed ? answer.getContent() : "LOCKED")
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
