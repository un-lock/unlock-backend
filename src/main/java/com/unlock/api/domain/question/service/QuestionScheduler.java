package com.unlock.api.domain.question.service;

import com.unlock.api.domain.couple.entity.Couple;
import com.unlock.api.domain.couple.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
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

    /**
     * 매 분 0초마다 실행
     * 알림 시간이 현재 시간인 커플들을 찾아 질문 배정 및 알림 발송
     */
    @Scheduled(cron = "0 * * * * *")
    public void scheduleDailyQuestions() {
        // 초를 제외한 시:분 정보 가져오기
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        log.info("자동 질문 배정 스케줄러 작동 중... 기준 시간: {}", now);

        // 해당 시간이 알림 시간인 커플 조회
        List<Couple> targetCouples = coupleRepository.findAllByNotificationTime(now);

        for (Couple couple : targetCouples) {
            try {
                // 질문 배정 (이미 배정되었다면 QuestionService 내부에서 처리됨)
                questionService.assignQuestionToCouple(couple);
                
                // TODO: 실제 푸시 알림 발송 로직 (FCM 등) 호출
                log.info("커플(ID:{})에게 알림 발송 성공! - 알림 시간: {}", couple.getId(), now);
            } catch (Exception e) {
                log.error("커플(ID:{}) 질문 배정 중 오류 발생: {}", couple.getId(), e.getMessage());
            }
        }
    }
}
