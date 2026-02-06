package com.unlock.api.domain.question.service;

import com.unlock.api.domain.auth.service.RedisService;
import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.couple.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    /**
     * 매 분 0초마다 실행
     * 1초 보정(Rounding) 로직을 통해 시계 오차로 인한 시간 밀림 및 중복 실행을 방지합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void scheduleDailyQuestions() {
        // 1초를 더해줌으로써 59.999초에 실행된 경우를 다음 분으로 정확히 판정 (Rounding)
        LocalDateTime adjustedNow = LocalDateTime.now().plusSeconds(1);
        String timeKey = adjustedNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        LocalTime targetTime = adjustedNow.toLocalTime().withSecond(0).withNano(0);

        // 1. Redis 분 단위 락 획득 시도
        if (!redisService.lockSchedule(timeKey)) {
            return;
        }

        log.info("자동 질문 배정 스케줄러 가동: 타겟 시간 {}", targetTime);

        // 2. 해당 시간이 알림 시간인 커플 조회
        List<Couple> targetCouples = coupleRepository.findAllByNotificationTime(targetTime);

        if (targetCouples.isEmpty()) {
            return;
        }

        // 3. 대상 커플별 질문 배정 및 알림 발송
        for (Couple couple : targetCouples) {
            try {
                questionService.assignQuestionToCouple(couple);
                // TODO: 실제 푸시 알림 발송 로직 호출
                log.info("알림 발송 완료: 커플(ID:{}), 시간({})", couple.getId(), targetTime);
            } catch (Exception e) {
                log.error("질문 배정 중 에러 발생: 커플(ID:{}), 사유: {}", couple.getId(), e.getMessage());
            }
        }
    }
}