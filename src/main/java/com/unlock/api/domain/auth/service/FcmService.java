package com.unlock.api.domain.auth.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FCM 푸시 알림 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    /**
     * 단일 기기에 푸시 알림을 발송합니다. (비동기)
     */
    @Async
    public void sendMessage(String targetToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 알림 발송 성공: {}", response);
        } catch (Exception e) {
            log.error("FCM 알림 발송 실패 (Token: {}): {}", targetToken, e.getMessage());
        }
    }

    /**
     * 여러 기기에 푸시 알림을 발송합니다.
     */
    @Async
    public void sendMessages(List<String> targetTokens, String title, String body) {
        if (targetTokens.isEmpty()) return;

        List<Message> messages = targetTokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build())
                .collect(Collectors.toList());

        try {
            FirebaseMessaging.getInstance().sendAll(messages);
            log.info("{}개의 기기에 FCM 알림 발송 완료", targetTokens.size());
        } catch (Exception e) {
            log.error("FCM 다중 알림 발송 중 에러 발생: {}", e.getMessage());
        }
    }
}
