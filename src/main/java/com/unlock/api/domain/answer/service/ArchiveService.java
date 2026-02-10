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
import java.util.Map;
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
     * [성능 최적화]: 루프 내 쿼리(N+1) 방지를 위해 In-Query 및 메모리 매핑 기법 사용
     */
    public List<ArchiveSummaryResponse> getMonthlyArchive(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        // 1. 특정 년/월의 질문 배정 기록 일괄 조회
        List<CoupleQuestion> questions = coupleQuestionRepository.findAllByCoupleAndYearAndMonth(couple, year, month);
        
        // 2. 질문 ID 리스트 추출
        List<Long> questionIds = questions.stream()
                .map(cq -> cq.getQuestion().getId())
                .collect(Collectors.toList());

        if (questionIds.isEmpty()) return List.of();

        // 3. [N+1 해결] 이번 달 답변 현황을 단 두 번의 쿼리로 일괄 조회
        Set<Long> myAnsweredQuestionIds = answerRepository.findAllByUserAndQuestionIds(user, questionIds).stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        Set<Long> partnerAnsweredQuestionIds = answerRepository.findAllByUserAndQuestionIds(partner, questionIds).stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        // 4. 메모리(Set) 대조를 통해 DTO 생성 (DB 추가 조회 없음)
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

        // 해당 커플에게 배정된 질문인지 검증
        List<CoupleQuestion> history = coupleQuestionRepository.findAllByCoupleOrderByAssignedDateDesc(couple);
        CoupleQuestion targetCq = history.stream()
                .filter(cq -> cq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 내 답변과 파트너 답변 조회
        Answer myAnswer = answerRepository.findByUserAndQuestion(user, targetCq.getQuestion()).orElse(null);
        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, targetCq.getQuestion()).orElse(null);

        // 열람 권한 체크
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