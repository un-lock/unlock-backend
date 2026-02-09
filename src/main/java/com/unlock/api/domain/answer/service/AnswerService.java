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
 * 답변 등록 및 열람 권한 관리 비즈니스 로직 서비스
 * 
 * 주요 정책:
 * - 상호성(Reciprocity): 내가 답변을 완료해야만 파트너의 상태를 확인할 수 있습니다.
 * - 수익화 연동: 파트너의 답변을 열람하려면 광고 시청 기록이 있거나 프리미엄 구독 상태여야 합니다.
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
     * 1. 오늘 우리 커플에게 배정된 질문을 찾습니다.
     * 2. 이미 답변을 남겼는지 확인 후 저장합니다.
     */
    public void submitAnswer(Long userId, AnswerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 오늘 날짜로 배정(또는 이동)된 질문 조회
        CoupleQuestion coupleQuestion = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 중복 답변 방지
        if (answerRepository.existsByUserAndQuestion(user, coupleQuestion.getQuestion())) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        Answer answer = Answer.builder()
                .user(user)
                .question(coupleQuestion.getQuestion())
                .content(request.getContent())
                .build();

        answerRepository.save(answer);

        // TODO: [Push Notification] 파트너에게 "상대방이 답변을 완료했습니다! 확인하러 가볼까요? 🔓" 알림 발송
    }

    /**
     * 오늘의 답변 현황 조회 (나와 파트너)
     * 파트너의 답변 내용은 열람 권한(광고/구독) 여부에 따라 마스킹 처리될 수 있습니다.
     * 
     * @return 나의 답변 상세와 파트너의 답변 요약(또는 상세) 정보를 담은 DTO
     */
    @Transactional(readOnly = true)
    public TodayAnswerResponse getTodayAnswers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        CoupleQuestion coupleQuestion = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 1. 내 답변 조회 (미작성 시 파트너 답변 조회를 차단하기 위해 예외 발생)
        Answer myAnswer = answerRepository.findByUserAndQuestion(user, coupleQuestion.getQuestion())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_ANSWER_LOCKED));

        // 2. 파트너 답변 조회
        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, coupleQuestion.getQuestion()).orElse(null);

        // 3. 열람 권한 체크 (구독 중이거나, 이미 광고를 시청하여 해제했는지 확인)
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
     * 파트너 답변 잠금 해제 (Unlock)
     * 광고 시청 완료 시 호출되며, 해당 답변에 대한 영구적인 열람 권한을 기록합니다.
     */
    public void revealPartnerAnswer(Long userId, Long answerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. 해제 대상 답변 존재 확인
        Answer targetAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        // 2. 보안 검증: 본인 답변은 해제 불필요
        if (targetAnswer.getUser().getId().equals(userId)) {
            throw new BusinessException("자신의 답변은 해제할 필요가 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        // 3. 보안 검증: 실제 내 파트너의 답변이 맞는지 확인
        Couple couple = user.getCouple();
        if (couple == null || !targetAnswer.getUser().getCouple().getId().equals(couple.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4. 비즈니스 검증: 상대방이 답변을 실제로 완료했는지 확인
        if (targetAnswer.getContent() == null || targetAnswer.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARTNER_NOT_ANSWERED);
        }

        // 5. 열람 기록 저장 (중복 기록 방지)
        if (!answerRevealRepository.existsByUserAndAnswer(user, targetAnswer)) {
            answerRevealRepository.save(AnswerReveal.builder()
                    .user(user)
                    .answer(targetAnswer)
                    .build());
        }
    }

    /**
     * Entity -> MyAnswerDto 변환 (상세 내용 포함)
     */
    private MyAnswerDto convertToMyAnswerDto(Answer answer) {
        return MyAnswerDto.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }

    /**
     * Entity -> PartnerAnswerDto 변환 (권한에 따른 마스킹 처리)
     */
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
                .content(isRevealed ? answer.getContent() : "LOCKED") // 권한 없으면 마스킹
                .createdAt(answer.getCreatedAt())
                .build();
    }
}