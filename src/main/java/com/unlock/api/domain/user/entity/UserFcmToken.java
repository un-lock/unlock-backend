package com.unlock.api.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유저별 FCM 토큰 관리 엔티티 (멀티 디바이스 지원)
 */
@Entity
@Table(name = "user_fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 토큰 소유 유저

    @Column(nullable = false, unique = true)
    private String token; // FCM 기기 토큰

    @Column(nullable = false)
    private LocalDateTime lastUsedAt; // 마지막 사용(갱신) 시각

    /**
     * 토큰 사용 시각을 현재로 업데이트합니다.
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
