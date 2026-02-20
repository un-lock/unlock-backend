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
 * 이메일 발송 및 인증 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisService redisService;
    private final UserRepository userRepository;

    /**
     * 회원가입용 인증번호 발송
     */
    public void sendVerificationEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        sendCode(email, "[un:lock] 회원가입 인증번호 안내");
    }

    /**
     * 비밀번호 재설정용 인증번호 발송
     */
    public void sendPasswordResetCode(String email) {
        sendCode(email, "[un:lock] 비밀번호 재설정 인증번호 안내");
    }

    /**
     * 임시 비밀번호 발송
     */
    public void sendTemporaryPassword(String email, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[un:lock] 임시 비밀번호 안내");
        
        String content = """
                안녕하세요. un:lock 입니다.
                
                요청하신 임시 비밀번호가 발급되었습니다.
                임시 비밀번호: [%s]
                
                로그인 후 반드시 비밀번호를 변경해 주세요.
                """.formatted(tempPassword);
        
        message.setText(content);
        mailSender.send(message);
        log.info("임시 비밀번호 발송 성공: {}", email);
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

    /**
     * 공통 인증번호 발송 로직
     */
    private void sendCode(String email, String subject) {
        String verificationCode = generateCode();
        try {
            redisService.saveVerificationCode(email, verificationCode);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            
            String content = """
                    안녕하세요. un:lock 입니다.
                    
                    인증번호는 [%s] 입니다.
                    3분 이내에 입력해 주세요.
                    """.formatted(verificationCode);
            
            message.setText(content);
            mailSender.send(message);
            log.info("인증 이메일 발송 성공: {}", email);
        } catch (Exception e) {
            log.error("인증 이메일 발송 실패: ", e);
            redisService.deleteVerificationCode(email);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
