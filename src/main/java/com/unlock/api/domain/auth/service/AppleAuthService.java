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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Apple Sign In 소셜 로그인 서비스
 *
 * [로그인]
 * 1. 앱이 보낸 identityToken(JWT)을 받음
 * 2. Apple 공개키 목록(JWKS)을 Apple 서버에서 조회
 * 3. identityToken 헤더의 kid와 일치하는 공개키로 서명 검증
 * 4. 검증된 JWT의 payload에서 sub(socialId), email 추출
 * 5. authorizationCode를 Apple 서버와 교환해 refresh_token 저장 (탈퇴 대비)
 *
 * [탈퇴]
 * 1. client_secret(JWT) 생성 — .p8 키로 ES256 서명
 * 2. 저장된 refresh_token + client_secret으로 Apple revoke API 호출
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
    private static final String APPLE_TOKEN_URL       = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL      = "https://appleid.apple.com/auth/revoke";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 로그인: identityToken 검증 ──────────────────────────────────────────

    @Override
    public SocialProfile getProfile(String identityToken) {
        try {
            String kid = extractKidFromToken(identityToken);

            Map<String, Object> jwks = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

            Map<String, Object> matchingKey = keys.stream()
                    .filter(k -> kid.equals(k.get("kid")))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

            RSAPublicKey publicKey = buildRSAPublicKey(matchingKey);

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            String socialId = claims.getSubject();
            String email    = claims.get("email", String.class);

            // Apple은 최초 로그인 이후 email을 제공하지 않을 수 있음
            if (email == null || email.isBlank()) {
                email = socialId + "@apple.placeholder";
            }

            return SocialProfile.builder()
                    .socialId(socialId)
                    .email(email)
                    .nickname("애플유저")
                    .provider(AuthProvider.APPLE)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[APPLE] identityToken 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    // ── 로그인 후: authorizationCode → refresh_token 교환 ──────────────────

    @Override
    public String exchangeCode(String authorizationCode) {
        try {
            String clientSecret = generateClientSecret();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id",     clientId);
            params.add("client_secret", clientSecret);
            params.add("code",          authorizationCode);
            params.add("grant_type",    "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(APPLE_TOKEN_URL, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("refresh_token")) {
                log.warn("[APPLE] authorizationCode 교환 응답에 refresh_token 없음: {}", body);
                return null;
            }

            String refreshToken = (String) body.get("refresh_token");
            log.info("[APPLE] authorizationCode → refresh_token 교환 성공");
            return refreshToken;

        } catch (Exception e) {
            log.error("[APPLE] authorizationCode 교환 실패: {}", e.getMessage());
            return null;
        }
    }

    // ── 탈퇴: refresh_token으로 Apple revoke API 호출 ──────────────────────

    @Override
    public void unlink(String appleRefreshToken) {
        if (appleRefreshToken == null || appleRefreshToken.isBlank()) {
            log.warn("[APPLE] appleRefreshToken이 없어 revoke를 건너뜁니다.");
            return;
        }

        try {
            String clientSecret = generateClientSecret();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id",       clientId);
            params.add("client_secret",   clientSecret);
            params.add("token",           appleRefreshToken);
            params.add("token_type_hint", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            restTemplate.postForEntity(APPLE_REVOKE_URL, request, String.class);

            log.info("[APPLE] 연동 해제(revoke) 성공");

        } catch (Exception e) {
            // 탈퇴 흐름 자체를 막지 않기 위해 예외를 던지지 않고 로그만 남김
            log.error("[APPLE] 연동 해제(revoke) 실패: {}", e.getMessage());
        }
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.APPLE;
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────────────

    /**
     * JWT 헤더(Base64URL 디코딩)에서 kid 추출
     */
    private String extractKidFromToken(String token) throws Exception {
        String headerBase64 = token.split("\\.")[0];
        String headerJson   = new String(Base64.getUrlDecoder().decode(headerBase64));
        Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
        return (String) header.get("kid");
    }

    /**
     * Apple JWKS의 n, e 값으로 RSAPublicKey 생성
     */
    private RSAPublicKey buildRSAPublicKey(Map<String, Object> jwk) throws Exception {
        BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("n")));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("e")));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    /**
     * Apple API 호출에 필요한 client_secret JWT 생성
     * - 알고리즘: ES256 (.p8 EC 개인키로 서명)
     * - 유효기간: 6개월
     */
    private String generateClientSecret() throws Exception {
        String cleanKey = privateKeyStr
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes  = Base64.getDecoder().decode(cleanKey);
        PrivateKey privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        long now = System.currentTimeMillis() / 1000;

        return Jwts.builder()
                .header().add("kid", keyId).and()
                .issuer(teamId)
                .issuedAt(new Date(now * 1000))
                .expiration(new Date((now + 15_777_000L) * 1000)) // 약 6개월
                .audience().add("https://appleid.apple.com").and()
                .subject(clientId)
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }
}
