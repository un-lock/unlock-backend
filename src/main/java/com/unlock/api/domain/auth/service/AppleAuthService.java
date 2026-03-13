package com.unlock.api.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Apple Sign In 소셜 로그인 서비스
 *
 * 동작 방식:
 * 1. 앱이 보낸 identityToken(JWT)을 받음
 * 2. Apple 공개키 목록(JWKS)을 Apple 서버에서 조회
 * 3. identityToken 헤더의 kid와 일치하는 공개키로 서명 검증
 * 4. 검증된 JWT의 payload에서 sub(socialId), email 추출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService implements SocialAuthService {

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.client-id}")
    private String clientId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${apple.private-key}")
    private String privateKeyStr;

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SocialProfile getProfile(String identityToken) {
        try {
            // 1. JWT 헤더에서 kid 추출
            String kid = extractKidFromToken(identityToken);

            // 2. Apple 공개키 목록 조회
            Map<String, Object> jwks = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

            // 3. kid가 일치하는 공개키 탐색
            Map<String, Object> matchingKey = keys.stream()
                    .filter(k -> kid.equals(k.get("kid")))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

            // 4. RSA 공개키 생성
            RSAPublicKey publicKey = buildRSAPublicKey(matchingKey);

            // 5. JWT 서명 검증 및 클레임 추출
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            String socialId = claims.getSubject();
            String email = claims.get("email", String.class);

            // Apple은 최초 로그인 이후 email을 제공하지 않을 수 있음
            // email이 없으면 socialId 기반의 고유 식별자로 대체
            if (email == null || email.isBlank()) {
                email = socialId + "@apple.placeholder";
            }

            return SocialProfile.builder()
                    .socialId(socialId)
                    .email(email)
                    .nickname("애플유저")   // Apple은 닉네임을 제공하지 않음 — 앱에서 별도 입력 유도 필요
                    .provider(AuthProvider.APPLE)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[APPLE] identityToken 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * JWT 헤더(Base64URL 디코딩)에서 kid 값 추출
     */
    private String extractKidFromToken(String token) throws Exception {
        String headerBase64 = token.split("\\.")[0];
        String headerJson = new String(Base64.getUrlDecoder().decode(headerBase64));
        Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
        return (String) header.get("kid");
    }

    /**
     * Apple JWKS의 n, e 값으로 RSAPublicKey 생성
     */
    private RSAPublicKey buildRSAPublicKey(Map<String, Object> jwk) throws Exception {
        BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("n")));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("e")));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /**
     * Apple 연동 해제
     * - Apple 탈퇴는 authorizationCode → refresh_token 교환 후 revoke API 호출 필요
     * - 현재는 로그 처리만 수행 (추후 구현 예정)
     */
    @Override
    public void unlink(String socialId) {
        log.info("[APPLE] 연결 끊기 요청 - socialId: {} (Apple revoke API 미구현, 추후 처리 필요)", socialId);
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.APPLE;
    }
}