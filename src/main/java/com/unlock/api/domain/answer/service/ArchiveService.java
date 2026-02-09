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

import java.time.LocalDateTime;
import java.util.List;
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
     * 전체 아카이브 목록 조회 (최신순)
     * - 캘린더 구성을 위해 날짜, 질문ID, 두 사람의 답변 여부 반환
     */
    public List<ArchiveSummaryResponse> getArchiveList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        return coupleQuestionRepository.findAllByCoupleOrderByAssignedDateDesc(couple)
                .stream()
                .map(cq -> ArchiveSummaryResponse.builder()
                        .questionId(cq.getQuestion().getId())
                        .questionContent(cq.getQuestion().getContent())
                        .date(cq.getAssignedDate())
                        .myAnswered(answerRepository.existsByUserAndQuestion(user, cq.getQuestion()))
                        .partnerAnswered(answerRepository.existsByUserAndQuestion(partner, cq.getQuestion()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 아카이브 상세 조회
     * - 특정 질문에 대한 두 사람의 답변 상세 내용 반환
     * - 잠금 로직(본인 미등록 시 차단, 광고/구독 체크) 동일 적용
     */
    public ArchiveDetailResponse getArchiveDetail(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 해당 커플에게 배정된 질문인지 검증하며 조회
        List<CoupleQuestion> history = coupleQuestionRepository.findAllByCoupleOrderByAssignedDateDesc(couple);
        CoupleQuestion targetCq = history.stream()
                .filter(cq -> cq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 1. 내 답변 조회
        Answer myAnswer = answerRepository.findByUserAndQuestion(user, targetCq.getQuestion()).orElse(null);
        
        // 2. 파트너 정보 및 답변 조회
        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, targetCq.getQuestion()).orElse(null);

        // 3. 열람 권한 체크
        boolean isRevealed = false;
        if (myAnswer != null && partnerAnswer != null) {
            isRevealed = couple.isSubscribed() || answerRevealRepository.existsByUserAndAnswer(user, partnerAnswer);
        }

        return ArchiveDetailResponse.builder()
                .questionContent(targetCq.getQuestion().getContent())
                .category(targetCq.getQuestion().getCategory().getDescription())
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