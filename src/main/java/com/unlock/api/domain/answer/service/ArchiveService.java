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
import java.util.stream.Collectors;

/**
 * 아카이브(기록장) 조회 비즈니스 로직 서비스
 * 
 * 주요 기능:
 * - 캘린더 구성을 위한 월별 요약 조회
 * - 특정 날짜의 질문 및 두 사람의 답변 상세 조회
 * - 아카이브 내에서도 답변 잠금 정책은 동일하게 유지
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
     * 프론트엔드에서 캘린더에 점을 찍기 위한 최소한의 데이터(날짜, 답변여부)를 반환합니다.
     * 
     * @param userId 유저 ID
     * @param year 대상 년도
     * @param month 대상 월
     * @return 특정 월의 질문 배정 이력 리스트
     */
    public List<ArchiveSummaryResponse> getMonthlyArchive(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();

        // 지정된 년/월에 해당하는 커플의 질문 배정 기록 조회
        return coupleQuestionRepository.findAllByCoupleAndYearAndMonth(couple, year, month)
                .stream()
                .map(cq -> {
                    boolean myAnswered = answerRepository.existsByUserAndQuestion(user, cq.getQuestion());
                    boolean partnerAnswered = answerRepository.existsByUserAndQuestion(partner, cq.getQuestion());
                    
                    return ArchiveSummaryResponse.builder()
                            .questionId(cq.getQuestion().getId())
                            .questionContent(cq.getQuestion().getContent())
                            .date(cq.getAssignedDate())
                            .myAnswered(myAnswered)
                            .partnerAnswered(partnerAnswered)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 아카이브 상세 조회
     * 특정 질문에 대한 두 사람의 답변 상세 내용을 반환합니다.
     * 
     * @param questionId 상세 조회할 질문 ID
     * @throws BusinessException 우리 커플에게 배정되지 않은 질문일 경우 발생
     */
    public ArchiveDetailResponse getArchiveDetail(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Couple couple = user.getCouple();
        if (couple == null) throw new BusinessException(ErrorCode.COUPLE_NOT_FOUND);

        // 1. 해당 질문이 우리 커플에게 배정된 이력이 있는지 검증
        List<CoupleQuestion> history = coupleQuestionRepository.findAllByCoupleOrderByAssignedDateDesc(couple);
        CoupleQuestion targetCq = history.stream()
                .filter(cq -> cq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 2. 내 답변과 파트너 답변을 각각 조회
        Answer myAnswer = answerRepository.findByUserAndQuestion(user, targetCq.getQuestion()).orElse(null);
        User partner = couple.getUser1().getId().equals(userId) ? couple.getUser2() : couple.getUser1();
        Answer partnerAnswer = answerRepository.findByUserAndQuestion(partner, targetCq.getQuestion()).orElse(null);

        // 3. 열람 권한 판단 (내가 작성 완료 + (구독 중이거나 광고 해제 완료))
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

    /**
     * Entity -> DTO 변환 (내 답변)
     */
    private MyAnswerDto convertToMyAnswerDto(Answer answer) {
        return MyAnswerDto.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (파트너 답변 - 마스킹 적용)
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
                .content(isRevealed ? answer.getContent() : "LOCKED")
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
