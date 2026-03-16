package com.unlock.api.domain.admob.controller;

import com.unlock.api.domain.admob.service.AdmobSsvService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Google AdMob SSV 콜백 수신 컨트롤러
 *
 * Google이 광고 완료 시 직접 호출하는 엔드포인트.
 * 반드시 200을 반환해야 하며, 200이 아닌 경우 Google이 재시도함.
 */
@Hidden
@Slf4j
@RestController
@RequestMapping("/api/v1/admob")
@RequiredArgsConstructor
public class AdmobSsvController {

    private final AdmobSsvService admobSsvService;

    /**
     * AdMob SSV 콜백
     * Google이 광고 완료 시 호출: GET /api/v1/admob/ssv?ad_network=...&custom_data=...&key_id=...&signature=...
     */
    @GetMapping("/ssv")
    public ResponseEntity<String> handleSsv(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        String rawQueryString = request.getQueryString();
        log.info("[SSV] 콜백 수신 - transactionId: {}", params.get("transaction_id"));

        try {
            admobSsvService.processSsvCallback(rawQueryString, params);
        } catch (Exception e) {
            // Google에는 항상 200 반환 (아니면 Google이 계속 재시도함)
            log.error("[SSV] 처리 중 예외 발생: {}", e.getMessage());
        }

        return ResponseEntity.ok("OK");
    }
}
