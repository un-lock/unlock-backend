package com.unlock.api.domain.user.repository;

import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * FCM 토큰 레포지토리
 */
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {
    
    /**
     * 특정 토큰 값으로 엔티티 조회
     */
    Optional<UserFcmToken> findByToken(String token);

    /**
     * 특정 유저의 모든 토큰 조회 (알림 발송용)
     */
    List<UserFcmToken> findAllByUser(User user);

    /**
     * 특정 유저의 특정 토큰 삭제 (로그아웃용)
     */
    void deleteByUserAndToken(User user, String token);

    /**
     * 특정 유저의 모든 토큰 삭제 (회원 탈퇴용)
     */
    void deleteAllByUser(User user);
}
