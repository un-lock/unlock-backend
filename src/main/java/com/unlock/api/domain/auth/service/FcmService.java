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
     * 특정 유저에게 Silent Push를 발송합니다. (알림 트레이에 표시 안됨)
     * 앱이 포그라운드/백그라운드에서 조용히 수신하여 화면을 갱신할 때 사용합니다.
     * 예: 광고 시청 후 파트너 답변 unlock 완료 신호
     */
    @Async
    public void sendSilentToUser(User user, NotificationType type) {
        List<String> tokens = fcmTokenRepository.findAllByUser(user).stream()
                .map(UserFcmToken::getToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) return;

        List<Message> messages = tokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .putData("type", type.name()) // 알림 없이 데이터만 전송
                        .build())
                .collect(Collectors.toList());

        try {
            FirebaseMessaging.getInstance().sendEach(messages);
            log.info("[FCM] Silent Push {} 건 발송 완료 (Type: {})", tokens.size(), type);
        } catch (Exception e) {
            log.error("[FCM] Silent Push 발송 에러: {}", e.getMessage());
        }
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
