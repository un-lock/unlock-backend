package com.unlock.api.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인프라 건강검진(Health Check)을 위한 전용 컨트롤러
 */
@Hidden // Swagger 문서에는 노출하지 않음
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
