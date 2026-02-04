package com.unlock.api.domain.user.entity;

import com.unlock.api.domain.common.BaseTimeEntity;
import com.unlock.api.domain.couple.entity.Couple;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 정보를 담는 엔티티
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

    @Column(unique = true)
    private String socialId; // 소셜 로그인 고유 식별자 (소셜 로그인인 경우)

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email; // 이메일 (로그인 ID 겸용)

    private String password; // 이메일 로그인을 위한 비밀번호 (소셜인 경우 null 가능)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider; // 인증 제공자 (KAKAO, GOOGLE, APPLE, EMAIL)

    @Column(unique = true)
    private String inviteCode; // 커플 연결을 위한 초대 코드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple; // 소속된 커플 정보

    /**
     * 초대 코드 설정
     */
    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    /**
     * 커플 연결 설정
     */
    public void setCouple(Couple couple) {
        this.couple = couple;
    }
}
