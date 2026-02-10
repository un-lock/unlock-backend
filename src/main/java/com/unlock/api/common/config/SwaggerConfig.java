package com.unlock.api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger(springdoc) 설정 클래스
 * - 로드밸런서 환경 서버 주소 설정
 * - JWT(Bearer) 인증 버튼 추가 및 전역 적용
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "Bearer Auth";

    @Bean
    public OpenAPI unlockOpenAPI() {
        // 1. 서버 주소 설정 (포트 누락 방지)
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("운영(Prod) 환경 (8080)");

        Server devServer = new Server();
        devServer.setUrl("http://localhost:8081");
        devServer.setDescription("개발(Dev) 환경 (8081)");

        // 2. JWT 인증 설정 (상단 자물쇠 버튼 생성)
        SecurityScheme securityScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("로그인 후 발급받은 AccessToken을 입력해 주세요.");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(BEARER_AUTH);

        return new OpenAPI()
                .servers(List.of(localServer, devServer))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes(BEARER_AUTH, securityScheme))
                .info(new Info()
                        .title("un:lock API")
                        .description("우리만의 은밀한 대화, un:lock 서비스의 API 문서입니다.")
                        .version("v0.0.1"));
    }
}
