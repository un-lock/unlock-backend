package com.unlock.api.domain.answer.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.answer.dto.AnswerDto.MyAnswerDto;
import com.unlock.api.domain.answer.dto.AnswerDto.PartnerAnswerDto;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveDetailResponse;
import com.unlock.api.domain.answer.dto.ArchiveDto.ArchiveSummaryResponse;
import com.unlock.api.domain.answer.entity.Answer;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 아카이브(기록장) 조회 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArchiveService {

    private final AnswerRepository answerRepository;
    private final AnswerRevealRepository answerRevealRepository;
    private final CoupleQuestionRepository coupleQuestionRepository;
    private final UserRepository userRepository;

    /**
     * 월별 아카이브 요약 목록 조회 (캘린더용)
     */
    public List<ArchiveSummaryResponse> getMonthlyArchive(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        List<CoupleQuestion> questions = coupleQuestionRepository.findAllByCoupleAndYearAndMonth(couple, year, month);
        
        List<Long> questionIds = questions.stream()
                .map(cq -> cq.getQuestion().getId())
                .collect(Collectors.toList());

        if (questionIds.isEmpty()) return List.of();

        Set<Long> myAnsweredQuestionIds = answerRepository.findAllByUserAndQuestionIds(user, questionIds).stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        Set<Long> partnerAnsweredQuestionIds = answerRepository.findAllByUserAndQuestionIds(partner, questionIds).stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        return questions.stream()
                .map(cq -> ArchiveSummaryResponse.builder()
                        .questionId(cq.getQuestion().getId())
                        .questionContent(cq.getQuestion().getContent())
                        .date(cq.getAssignedDate())
                        .myAnswered(myAnsweredQuestionIds.contains(cq.getQuestion().getId()))
                        .partnerAnswered(partnerAnsweredQuestionIds.contains(cq.getQuestion().getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 아카이브 상세 조회
     */
    public ArchiveDetailResponse getArchiveDetail(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        List<CoupleQuestion> history = coupleQuestionRepository.findAllByCoupleOrderByAssignedDateDesc(couple);
        CoupleQuestion targetCq = history.stream()
                .filter(cq -> cq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        Answer myAnswer = answerRepository.findByUserAndQuestion(user, targetCq.getQuestion()).orElse(null);
        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, targetCq.getQuestion()).orElse(null);

        boolean isRevealed = false;
        if (myAnswer != null && partnerAnswer != null) {
            isRevealed = couple.isSubscribed() || answerRevealRepository.existsByUserAndAnswer(user, partnerAnswer);
        }

        return ArchiveDetailResponse.builder()
                .questionContent(targetCq.getQuestion().getContent())
                .category(targetCq.getQuestion().getCategory()) // Enum 타입 그대로 반환
                .date(targetCq.getAssignedDate())
                .myAnswer(myAnswer == null ? null : convertToMyAnswerDto(myAnswer))
                .partnerAnswer(convertToPartnerAnswerDto(partner, partnerAnswer, isRevealed))
                .build();
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
