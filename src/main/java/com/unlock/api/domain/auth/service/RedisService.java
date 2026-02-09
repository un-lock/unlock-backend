package com.unlock.api.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis를 활용한 데이터 관리 서비스 (주로 이메일 인증번호 저장 용도)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 인증번호 저장 (3분간 유효)
     */
    public void saveVerificationCode(String email, String code) {
        String key = "AUTH:" + email;
        redisTemplate.opsForValue().set(key, code, 3, TimeUnit.MINUTES);
        log.info("Redis 저장 완료 - Key: {}, Value: {}", key, code);
    }

    /**
     * 인증번호 조회
     */
    public String getVerificationCode(String email) {
        return redisTemplate.opsForValue().get("AUTH:" + email);
    }

    /**
     * 인증번호 삭제
     */
    public void deleteVerificationCode(String email) {
        redisTemplate.delete("AUTH:" + email);
    }

    /**
     * Refresh Token 저장
     */
    public void saveRefreshToken(Long userId, String refreshToken, long expirationTime) {
        String key = "RT:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expirationTime, TimeUnit.MILLISECONDS);
        log.info("Redis RefreshToken 저장 완료 - Key: {}", key);
    }

    /**
     * Refresh Token 조회
     */
    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get("RT:" + userId);
    }

    /**
     * Refresh Token 삭제 (로그아웃 시)
     */
    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete("RT:" + userId);
    }

    /**
     * 특정 시간에 대한 작업 락 획득 (중복 실행 방지)
     * @return 락 획득 성공 시 true, 이미 있으면 false
     */
    public boolean lockSchedule(String timeKey) {
        String key = "LOCK:" + timeKey;
        // setIfAbsent는 값이 없을 때만 저장함 (Atomic)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", 59, TimeUnit.SECONDS);
        return success != null && success;
    }

    /**
     * 커플 연결 신청 저장 (24시간 유효)
     */
    public void saveCoupleRequest(Long targetUserId, Long requesterId) {
        redisTemplate.opsForValue().set(
                "CP_REQ:" + targetUserId,
                requesterId.toString(),
                24,
                TimeUnit.HOURS
        );
    }

    /**
     * 커플 연결 신청 조회
     */
    public String getCoupleRequest(Long targetUserId) {
        return redisTemplate.opsForValue().get("CP_REQ:" + targetUserId);
    }

    /**
     * 커플 연결 신청 삭제 (수락/거절 시)
     */
    public void deleteCoupleRequest(Long targetUserId) {
        redisTemplate.delete("CP_REQ:" + targetUserId);
    }
}
