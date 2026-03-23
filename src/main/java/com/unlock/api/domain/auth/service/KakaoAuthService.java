package com.unlock.api.domain.auth.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService implements SocialAuthService {

    @Value("${kakao.admin-key}")
    private String adminKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public SocialProfile getProfile(String token) {
        // Kakao API 호출하여 프로필 정보 가져오기
        String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            String id = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = (String) kakaoAccount.get("email");
            String nickname = (String) profile.get("nickname");

            return SocialProfile.builder()
                    .socialId(id)
                    .email(email)
                    .nickname(nickname)
                    .provider(AuthProvider.KAKAO)
                    .build();
        } catch (Exception e) {
            log.error("[KAKAO] 프로필 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public void unlink(String socialId) {
        String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey); // Admin Key 방식
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", socialId); // 탈퇴할 사용자의 카카오 고유 ID

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(KAKAO_UNLINK_URL, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("[KAKAO] 연결 끊기 성공 - socialId: {}", socialId);
            } else {
                log.warn("[KAKAO] 연결 끊기 응답 이상 - status: {}, body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("[KAKAO] 연결 끊기 API 호출 실패 - socialId: {}, error: {}", socialId, e.getMessage());
            // 탈퇴 과정 자체를 막지 않기 위해 예외를 던지지 않고 로그만 남깁니다.
        }
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }
}
