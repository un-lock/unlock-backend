package com.unlock.api.domain.admob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlock.api.domain.answer.service.AnswerService;
import com.unlock.api.domain.auth.entity.NotificationType;
import com.unlock.api.domain.auth.service.FcmService;
import com.unlock.api.domain.auth.service.RedisService;
import com.unlock.api.domain.user.entity.User;
import com.unlock.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google AdMob SSV(Server-Side Verification) 서비스
 *
 * [흐름]
 * 1. Google이 광고 완료 시 우리 서버로 GET 콜백 전송
 * 2. 콜백의 signature를 Google 공개키로 검증
 * 3. custom_data에서 userId:answerId 파싱
 * 4. 중복 처리 방지 후 unlock 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdmobSsvService {

    private static final String GOOGLE_PUBLIC_KEYS_URL =
            "https://www.gstatic.com/admob/reward/verifier-keys.json";
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24시간

    private final AnswerService answerService;
    private final RedisService redisService;
    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // 공개키 캐시 (keyId → PublicKey)
    private final Map<Long, PublicKey> keyCache = new ConcurrentHashMap<>();
    private long keyCachedAt = 0;

    /**
     * SSV 콜백 처리 진입점
     *
     * @param rawQueryString 전체 쿼리 스트링 (ex: "ad_network=...&key_id=...&signature=...")
     * @param params         파싱된 파라미터 Map
     */
    public void processSsvCallback(String rawQueryString, Map<String, String> params) {
        String transactionId = params.get("transaction_id");
        String customData    = params.get("custom_data");
        String signatureB64  = params.get("signature");
        String keyIdStr      = params.get("key_id");

        // 1. 필수 파라미터 확인
        if (transactionId == null || signatureB64 == null || keyIdStr == null || customData == null) {
            log.warn("[SSV] 필수 파라미터 누락 - params: {}", params);
            return;
        }

        // 2. 중복 처리 방지
        if (redisService.isAdmobTransactionProcessed(transactionId)) {
            log.info("[SSV] 이미 처리된 transaction_id: {}", transactionId);
            return;
        }

        // 3. Google 서명 검증
        // message = 쿼리 스트링에서 "&key_id=..." 이전까지
        int keyIdIndex = rawQueryString.indexOf("&key_id=");
        if (keyIdIndex == -1) {
            log.warn("[SSV] 쿼리 스트링에서 key_id를 찾을 수 없음");
            return;
        }
        String message = rawQueryString.substring(0, keyIdIndex);
        long keyId = Long.parseLong(keyIdStr);
        log.info("[SSV] 서명 검증 대상 message: {}", message);

        if (!verifySignature(message, keyId, signatureB64)) {
            log.warn("[SSV] 서명 검증 실패 - transactionId: {}", transactionId);
            return;
        }

        // 4. custom_data 파싱: "userId:answerId" 형식
        String[] parts = customData.split(":");
        if (parts.length != 2) {
            log.warn("[SSV] custom_data 형식 오류: {}", customData);
            return;
        }
        long userId   = Long.parseLong(parts[0]);
        long answerId = Long.parseLong(parts[1]);

        // 5. unlock 처리
        try {
            answerService.unlockByAd(userId, answerId);
            redisService.markAdmobTransactionProcessed(transactionId);
            log.info("[SSV] unlock 완료 - userId: {}, answerId: {}, transactionId: {}", userId, answerId, transactionId);

            // 6. Silent Push → 알림 트레이에 표시 없이 앱이 조용히 화면 갱신
            userRepository.findById(userId).ifPresent(user ->
                    fcmService.sendSilentToUser(user, NotificationType.ANSWER_UNLOCKED)
            );
        } catch (Exception e) {
            log.error("[SSV] unlock 처리 중 오류 - userId: {}, answerId: {}, error: {}", userId, answerId, e.getMessage());
        }
    }

    // ── 서명 검증 ──────────────────────────────────────────────────────────────

    private boolean verifySignature(String message, long keyId, String signatureBase64Url) {
        try {
            PublicKey publicKey = getPublicKey(keyId);
            if (publicKey == null) {
                log.warn("[SSV] keyId {}에 해당하는 공개키 없음", keyId);
                return false;
            }

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureBase64Url);
            return sig.verify(signatureBytes);

        } catch (Exception e) {
            log.error("[SSV] 서명 검증 예외: {}", e.getMessage());
            return false;
        }
    }

    // ── Google 공개키 캐싱 ─────────────────────────────────────────────────────

    private PublicKey getPublicKey(long keyId) {
        refreshKeyCacheIfNeeded();
        return keyCache.get(keyId);
    }

    private void refreshKeyCacheIfNeeded() {
        if (!keyCache.isEmpty() && System.currentTimeMillis() - keyCachedAt < CACHE_TTL_MS) {
            return;
        }
        try {
            Map<String, Object> response = restTemplate.getForObject(GOOGLE_PUBLIC_KEYS_URL, Map.class);
            if (response == null) return;

            List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
            Map<Long, PublicKey> newCache = new ConcurrentHashMap<>();

            for (Map<String, Object> keyEntry : keys) {
                long id      = Long.parseLong(keyEntry.get("keyId").toString());
                String base64 = (String) keyEntry.get("base64");

                byte[] keyBytes = Base64.getDecoder().decode(base64);
                PublicKey publicKey = KeyFactory.getInstance("EC")
                        .generatePublic(new X509EncodedKeySpec(keyBytes));

                newCache.put(id, publicKey);
            }

            keyCache.clear();
            keyCache.putAll(newCache);
            keyCachedAt = System.currentTimeMillis();
            log.info("[SSV] Google 공개키 캐시 갱신 완료 - {} 개", newCache.size());

        } catch (Exception e) {
            log.error("[SSV] Google 공개키 로드 실패: {}", e.getMessage());
        }
    }
}
