package com.unlock.api.domain.couple.repository;

import com.unlock.api.domain.couple.entity.Couple;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
    
    /**
     * 특정 알림 시간을 가진 모든 커플 조회
     */
    List<Couple> findAllByNotificationTime(LocalTime notificationTime);
}
