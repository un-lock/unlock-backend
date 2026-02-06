package com.unlock.api.domain.question.service;

import com.unlock.api.domain.auth.service.RedisService;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.couple.repository.CoupleRepository;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.question.repository.CoupleQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 정해진 시간에 질문을 자동으로 배정하고 푸시 알림을 트리거하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionScheduler {

    private final CoupleRepository coupleRepository;
    private final QuestionService questionService;
    private final RedisService redisService;
    private final CoupleQuestionRepository coupleQuestionRepository;

    /**
     * 매 분 0초마다 실행
     */
    @Scheduled(cron = "0 * * * * *")
    public void scheduleDailyQuestions() {
        LocalDateTime adjustedNow = LocalDateTime.now().plusSeconds(1);
        String timeKey = adjustedNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        LocalTime targetTime = adjustedNow.toLocalTime().withSecond(0).withNano(0);

        if (!redisService.lockSchedule(timeKey)) {
            return;
        }

        List<Couple> targetCouples = coupleRepository.findAllByNotificationTime(targetTime);
        if (targetCouples.isEmpty()) return;

        for (Couple couple : targetCouples) {
            try {
                // 현재 배정되어 있던 질문 확인 (로직 실행 전 상태)
                boolean alreadyAssignedToday = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now()).isPresent();

                // 질문 배정 시도 (내부 로직에 의해 완료 여부 체크 후 배정됨)
                Question currentQuestion = questionService.assignQuestionToCouple(couple);
                
                // 오늘 날짜로 새로 배정되었는지 확인
                boolean newlyAssigned = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, LocalDate.now()).isPresent();

                if (!alreadyAssignedToday && newlyAssigned) {
                    log.info("[알림 발송] 커플(ID:{})님, 새로운 질문이 도착했습니다! 🔓", couple.getId());
                } else if (!newlyAssigned) {
                    log.info("[알림 발송] 커플(ID:{})님, 아직 완료하지 않은 질문이 있습니다. 답변을 남겨주세요! 🔔", couple.getId());
                }
                
            } catch (Exception e) {
                log.error("질문 알림 처리 중 에러 발생: 커플(ID:{}), 사유: {}", couple.getId(), e.getMessage());
            }
        }
    }
}
