package com.unlock.api.domain.auth.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.unlock.api.domain.auth.entity.NotificationType;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.entity.UserFcmToken;
import com.unlock.api.domain.user.repository.UserFcmTokenRepository;
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

    private final UserFcmTokenRepository fcmTokenRepository;

    /**
     * 특정 유저의 모든 등록된 기기에 푸시 알림을 발송합니다. (타입 포함)
     */
    @Async
    public void sendToUser(User user, String title, String body, NotificationType type) {
        List<String> tokens = fcmTokenRepository.findAllByUser(user).stream()
                .map(UserFcmToken::getToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            return;
        }

        sendMessages(tokens, title, body, type);
    }

    /**
     * 여러 기기에 푸시 알림을 발송합니다. (타입 정보 포함)
     */
    @Async
    public void sendMessages(List<String> targetTokens, String title, String body, NotificationType type) {
        if (targetTokens.isEmpty()) return;

        List<Message> messages = targetTokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type.name()) // 알림 타입 추가
                        .build())
                .collect(Collectors.toList());

        try {
            FirebaseMessaging.getInstance().sendEach(messages);
            log.info("[FCM] {} 건 발송 완료 (Type: {})", targetTokens.size(), type);
        } catch (Exception e) {
            log.error("[FCM] 발송 에러: {}", e.getMessage());
        }
    }
}
