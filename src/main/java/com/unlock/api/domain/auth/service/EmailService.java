package com.unlock.api.domain.auth.service;

import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    /**
     * 인증 이메일 발송
     */
    public void sendVerificationEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String verificationCode = generateCode();
        
        try {
            redisService.saveVerificationCode(email, verificationCode);
            log.info("Redis 인증번호 저장 성공: {} - code: {}", email, verificationCode);

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
            log.error("인증 프로세스 실패: ", e);
            redisService.deleteVerificationCode(email);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 인증번호 확인
     */
    public void verifyCode(String email, String code) {
        String savedCode = redisService.getVerificationCode(email);
        
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.AUTH_CODE_NOT_FOUND);
        }
        
        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }
        
        redisService.deleteVerificationCode(email);
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
