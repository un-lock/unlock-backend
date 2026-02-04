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
            
            // Redis에 저장
            redisService.saveVerificationCode(email, verificationCode);
            log.info("이메일 발송 성공: {} - code: {}", email, verificationCode);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
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
