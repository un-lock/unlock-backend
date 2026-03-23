package com.unlock.api.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlock.api.common.exception.BusinessException;
import com.unlock.api.common.exception.ErrorCode;
import com.unlock.api.domain.auth.dto.SocialProfile;
import com.unlock.api.domain.user.entity.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Google Sign In 소셜 로그인 서비스
 *
 * [로그인]
 * 1. 앱이 보낸 idToken을 Google tokeninfo API로 검증
 * 2. 검증된 응답에서 sub(socialId), email 추출
 * 3. serverAuthCode를 Google token API와 교환해 refresh_token 저장 (탈퇴 대비)
 *
 * [탈퇴]
 * 1. 저장된 refresh_token으로 Google revoke API 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService implements SocialAuthService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String GOOGLE_TOKEN_URL      = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_REVOKE_URL     = "https://oauth2.googleapis.com/revoke";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 로그인: idToken 검증 ────────────────────────────────────────────────

    @Override
    public SocialProfile getProfile(String idToken) {
        try {
            String url = GOOGLE_TOKEN_INFO_URL + "?id_token=" + idToken;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            // audience 검증: 우리 앱의 client_id와 일치해야 함
            String audience = (String) response.get("aud");
            if (!clientId.equals(audience)) {
                log.warn("[GOOGLE] audience 불일치 - expected: {}, actual: {}", clientId, audience);
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            String socialId = (String) response.get("sub");
            String email    = (String) response.get("email");

            if (socialId == null || email == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            return SocialProfile.builder()
                    .socialId(socialId)
                    .email(email)
                    .nickname("구글유저")
                    .provider(AuthProvider.GOOGLE)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GOOGLE] idToken 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    // ── 로그인 후: serverAuthCode → refresh_token 교환 ─────────────────────

    @Override
    public String exchangeCode(String serverAuthCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code",          serverAuthCode);
            params.add("client_id",     clientId);
            params.add("client_secret", clientSecret);
            params.add("grant_type",    "authorization_code");
            params.add("redirect_uri",  ""); // 네이티브 앱은 빈 값

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("refresh_token")) {
                log.warn("[GOOGLE] serverAuthCode 교환 응답에 refresh_token 없음: {}", body);
                return null;
            }

            String refreshToken = (String) body.get("refresh_token");
            log.debug("[GOOGLE] serverAuthCode → refresh_token 교환 성공");
            return refreshToken;

        } catch (Exception e) {
            log.error("[GOOGLE] serverAuthCode 교환 실패: {}", e.getMessage());
            return null;
        }
    }

    // ── 탈퇴: refresh_token으로 Google revoke API 호출 ──────────────────────

    @Override
    public void unlink(String googleRefreshToken) {
        if (googleRefreshToken == null || googleRefreshToken.isBlank()) {
            log.debug("[GOOGLE] googleRefreshToken이 없어 revoke를 건너뜁니다.");
            return;
        }

        try {
            String url = GOOGLE_REVOKE_URL + "?token=" + googleRefreshToken;
            restTemplate.postForEntity(url, null, String.class);
            log.debug("[GOOGLE] 연동 해제(revoke) 성공");
        } catch (Exception e) {
            // 클라이언트에서 revokeAccess()를 먼저 호출하므로, 서버 도달 시 이미 취소된 토큰일 수 있음
            log.debug("[GOOGLE] 연동 해제(revoke) 스킵 - 이미 클라이언트에서 취소되었거나 취소 불가 토큰: {}", e.getMessage());
        }
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }
}
