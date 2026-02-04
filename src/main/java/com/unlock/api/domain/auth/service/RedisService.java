package com.unlock.api.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis를 활용한 데이터 관리 서비스 (주로 이메일 인증번호 저장 용도)
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 인증번호 저장 (3분간 유효)
     */
    public void saveVerificationCode(String email, String code) {
        redisTemplate.opsForValue().set(
                "AUTH:" + email,
                code,
                3,
                TimeUnit.MINUTES
        );
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
}
