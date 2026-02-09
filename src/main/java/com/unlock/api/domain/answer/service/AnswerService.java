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

        // 1. 내 답변 조회 (내가 안 썼으면 파트너 답변은 아예 차단)
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. 대상 답변 존재 확인 (에러 코드 Q002 - ANSWER_NOT_FOUND 사용)
        Answer targetAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        // 2. 본인 답변 여부 체크
        if (targetAnswer.getUser().getId().equals(userId)) {
            throw new BusinessException("자신의 답변은 해제할 필요가 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        // 3. 파트너 관계 체크
        Couple couple = user.getCouple();
        if (couple == null || !targetAnswer.getUser().getCouple().getId().equals(couple.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4. 상대방이 답변을 썼는지 확인 (이미 1번에서 존재 여부 확인되지만 의미론적 보완)
        if (targetAnswer.getContent() == null || targetAnswer.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARTNER_NOT_ANSWERED);
        }

        // 5. 이미 해제되어 있는지 확인 후 저장
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