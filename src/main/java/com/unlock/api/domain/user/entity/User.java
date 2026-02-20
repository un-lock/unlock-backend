package com.unlock.api.domain.user.entity;

import com.unlock.api.domain.common.BaseTimeEntity;
import com.unlock.api.domain.couple.entity.Couple;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 기본 정보 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(unique = true)
    private String socialId; // 소셜 로그인 고유 식별자 (카카오 등)

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    // FCM 토큰 연관 관계 (1:N 멀티 디바이스 지원)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserFcmToken> fcmTokens = new ArrayList<>();

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 비밀번호를 새로운 값으로 업데이트합니다. (재설정 시 사용)
     */
    public void updatePassword(String password) {
        this.password = password;
    }

    /**
     * 초대 코드를 새로운 값으로 갱신합니다. (커플 해제 시 사용)
     */
    public void updateInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public void setCouple(Couple couple) {
        this.couple = couple;
    }

    public void clearCouple() {
        this.couple = null;
    }
}
