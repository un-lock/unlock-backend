package com.unlock.api.common.security.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JPA 엔티티 암호화 컨버터
 * DB 저장 시 AES-256 알고리즘으로 암호화하고, 조회 시 복호화합니다.
 */
@Converter
@Component
public class AesEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private final SecretKeySpec keySpec;

    public AesEncryptionConverter(@Value("${encryption.key}") String key) {
        // 공백 및 예기치 못한 줄바꿈 제거 후 정확히 32바이트 키 생성
        // String sanitizedKey = key.trim();
        this.keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return attribute;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("데이터 암호화 실패", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return dbData;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("데이터 복호화 실패", e);
        }
    }
}
