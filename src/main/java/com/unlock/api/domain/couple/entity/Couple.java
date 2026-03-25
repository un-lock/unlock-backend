package com.unlock.api.domain.couple.entity;

import com.unlock.api.domain.common.BaseTimeEntity;
import com.unlock.api.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 두 사용자의 연결 정보를 담는 엔티티 (커플)
 */
@Entity
@Table(name = "couples")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Couple extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id")
    private User user1; // 커플을 맺은 사용자 1

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2; // 커플을 맺은 사용자 2

    @Column(nullable = false)
    private LocalDate startDate; // 커플 시작일 (연결된 날짜)

    @Builder.Default
    @Column(nullable = false)
    private LocalTime notificationTime = LocalTime.of(22, 0); // 질문 알림 시간 (기본값 오후 10시)

    @Builder.Default
    @Column(nullable = false)
    private boolean isSubscribed = false; // 프리미엄 구독 여부 (true인 경우 광고 없이 답변 열람 가능)

    @Builder.Default
    @Column(nullable = false)
    private boolean isHotSpicyEnabled = false; // HOT_SPICY 카테고리 포함 여부 (프론트 버튼으로 토글)

    @Column(nullable = true)
    private LocalDate anniversaryDate; // 실제 사귄 날짜 (유저가 직접 입력, null 허용)

    public void updateNotificationTime(LocalTime newTime) {
        this.notificationTime = newTime;
    }

    public void updateHotSpicyEnabled(boolean enabled) {
        this.isHotSpicyEnabled = enabled;
    }

    public void updateAnniversaryDate(LocalDate anniversaryDate) {
        this.anniversaryDate = anniversaryDate;
    }

}