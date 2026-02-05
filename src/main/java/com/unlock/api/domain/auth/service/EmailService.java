package com.unlock.api.domain.auth.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 이메일 발송 및 인증 로직 담당 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisService redisService;

    /**
     * 인증 이메일 발송
     */
    public void sendVerificationEmail(String email) {
        String verificationCode = generateCode();
        
        try {
            // 1. Redis에 먼저 저장 (저장 실패 시 메일 안 보냄)
            redisService.saveVerificationCode(email, verificationCode);
            log.info("Redis 인증번호 저장 성공: {} - code: {}", email, verificationCode);

            // 2. 메일 발송
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[un:lock] 인증번호 안내");
            
            String content = """
                    안녕하세요. un:lock 입니다.
                    
                    인증번호는 [%s] 입니다.
                    3분 이내에 입력해 주세요.
                    """.formatted(verificationCode);
            
            message.setText(content);
            mailSender.send(message);
            
            log.info("이메일 발송 성공: {}", email);
        } catch (Exception e) {
            log.error("인증 프로세스 실패 (Redis 저장 또는 메일 발송 오류): ", e);
            // 메일 발송 실패 시 Redis 데이터 삭제 (선택 사항)
            redisService.deleteVerificationCode(email);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 인증번호 확인
     */
    public void verifyCode(String email, String code) {
        String savedCode = redisService.getVerificationCode(email);
        
        if (savedCode == null || !savedCode.equals(code)) {
            throw new BusinessException("인증번호가 일치하지 않거나 만료되었습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        
        // 확인 성공 시 삭제
        redisService.deleteVerificationCode(email);
    }

    /**
     * 6자리 난수 생성
     */
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}