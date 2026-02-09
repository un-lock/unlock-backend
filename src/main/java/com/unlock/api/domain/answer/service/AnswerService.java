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

        CoupleQuestion coupleQuestion = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        if (answerRepository.existsByUserAndQuestion(user, coupleQuestion.getQuestion())) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        Answer answer = Answer.builder()
                .user(user)
                .question(coupleQuestion.getQuestion())
                .content(request.getContent())
                .build();

        answerRepository.save(answer);

        // TODO: [Push Notification] 파트너를 찾아서 "상대방이 답변을 완료했습니다! 확인하러 가볼까요? 🔓" 알림 발송
        // (만약 파트너도 이미 답변을 쓴 상태라면 더 강력하게 유도 가능)
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

        Answer myAnswer = answerRepository.findByUserAndQuestion(user, coupleQuestion.getQuestion())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_ANSWER_LOCKED));

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, coupleQuestion.getQuestion()).orElse(null);

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Answer targetAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        if (targetAnswer.getUser().getId().equals(userId)) {
            throw new BusinessException("자신의 답변은 해제할 필요가 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        Couple couple = user.getCouple();
        if (couple == null || !targetAnswer.getUser().getCouple().getId().equals(couple.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (targetAnswer.getContent() == null || targetAnswer.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARTNER_NOT_ANSWERED);
        }

        if (!answerRevealRepository.existsByUserAndAnswer(user, targetAnswer)) {
            answerRevealRepository.save(AnswerReveal.builder()
                    .user(user)
                    .answer(targetAnswer)
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
