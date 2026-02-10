package com.unlock.api.domain.couple.repository;

import com.unlock.api.domain.couple.entity.Couple;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
    
    /**
     * 특정 알림 시간을 가진 모든 커플 조회 (유저 정보 포함 - N+1 방지)
     */
    @Query("SELECT DISTINCT c FROM Couple c " +
           "JOIN FETCH c.user1 " +
           "JOIN FETCH c.user2 " +
           "WHERE c.notificationTime = :notificationTime")
    List<Couple> findAllByNotificationTimeWithUsers(@Param("notificationTime") LocalTime notificationTime);

    /**
     * 특정 알림 시간을 가진 모든 커플 조회 (레거시)
     */
    List<Couple> findAllByNotificationTime(LocalTime notificationTime);
}
