package com.unlock.api.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Firebase Cloud Messaging(FCM) 최소 설정 클래스
 */
@Slf4j
@Configuration
public class FcmConfig {

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // 도커 볼륨 연결 경로 (/app/firebase-key.json)
                InputStream serviceAccount = new FileInputStream("/app/firebase-key.json");
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FCM 초기화 성공");
            }
        } catch (Exception e) {
            log.error("FCM 초기화 실패: {}", e.getMessage());
        }
    }
}
