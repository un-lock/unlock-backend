package com.unlock.api.domain.question.service;

import com.unlock.api.domain.answer.repository.AnswerRepository;
import com.unlock.api.domain.auth.entity.NotificationType;
import com.unlock.api.domain.auth.service.FcmService;
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
 * 정해진 시간에 질문을 자동으로 배정하고 사용자별 맞춤 알림을 트리거하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionScheduler {

    private final CoupleRepository coupleRepository;
    private final QuestionService questionService;
    private final RedisService redisService;
    private final CoupleQuestionRepository coupleQuestionRepository;
    private final AnswerRepository answerRepository;
    private final FcmService fcmService;

    @Scheduled(cron = "0 * * * * *")
    public void scheduleDailyQuestions() {
        LocalDateTime adjustedNow = LocalDateTime.now().plusSeconds(1);
        String timeKey = adjustedNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        LocalTime targetTime = adjustedNow.toLocalTime().withSecond(0).withNano(0);

        if (!redisService.lockSchedule(timeKey)) {
            return;
        }

        // 해당 시간이 알림 시간인 커플 조회 (User 정보까지 fetch join으로 한 번에 조회)
        List<Couple> targetCouplesFetch = coupleRepository.findAllByNotificationTimeWithUsers(targetTime);
        if (targetCouplesFetch.isEmpty()) return;

        log.info("[스케줄러] {}쌍의 커플 알림 처리 시작 (타겟: {})", targetCouplesFetch.size(), targetTime);

        for (Couple couple : targetCouplesFetch) {
            try {
                // 1. 현재 오늘 자로 배정된 질문이 있는지 사전 확인
                LocalDate today = LocalDate.now();
                boolean isNewQuestionDay = coupleQuestionRepository.findByCoupleAndAssignedDate(couple, today).isEmpty();

                // 2. 질문 배정/이동 처리 수행
                Question currentQuestion = questionService.assignQuestionToCouple(couple);

                // 3. 개별 유저별 답변 상태 체크
                boolean user1Finished = answerRepository.existsByUserAndQuestion(couple.getUser1(), currentQuestion);
                boolean user2Finished = answerRepository.existsByUserAndQuestion(couple.getUser2(), currentQuestion);

                // 4. 상황별 타겟 알림 발송
                if (user1Finished && user2Finished) {
                    log.info("[SKIP] 커플(ID:{}) - 두 분 모두 답변을 완료하여 알림을 보내지 않습니다.", couple.getId());
                    continue;
                }

                // [Case 1] 오늘 처음 질문이 배정되었거나 이동해온 경우 (둘 다 안 썼을 확률 높음)
                if (isNewQuestionDay && !user1Finished && !user2Finished) {
                    fcmService.sendToUser(couple.getUser1(), "un:lock 🔓", "오늘의 새로운 질문이 도착했습니다! 확인해 보세요.", NotificationType.DAILY_QUESTION);
                    fcmService.sendToUser(couple.getUser2(), "un:lock 🔓", "오늘의 새로운 질문이 도착했습니다! 확인해 보세요.", NotificationType.DAILY_QUESTION);
                } 
                else {
                    if (!user1Finished) {
                        String msg = user2Finished ? "파트너가 답변을 기다리고 있어요! 🔓" : "아직 오늘의 질문에 답변하지 않으셨어요! 🔔";
                        fcmService.sendToUser(couple.getUser1(), "un:lock 🔔", msg, NotificationType.DAILY_QUESTION);
                    }
                    if (!user2Finished) {
                        String msg = user1Finished ? "파트너가 답변을 기다리고 있어요! 🔓" : "아직 오늘의 질문에 답변하지 않으셨어요! 🔔";
                        fcmService.sendToUser(couple.getUser2(), "un:lock 🔔", msg, NotificationType.DAILY_QUESTION);
                    }
                }
                
            } catch (Exception e) {
                log.error("[스케줄러 에러] 커플(ID:{}) 처리 실패: {}", couple.getId(), e.getMessage());
            }
        }
    }
}
