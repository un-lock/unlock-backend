package com.unlock.api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger(springdoc) 설정 클래스
 * - 서버 URL 환경변수로 주입
 * - JWT(Bearer) 인증 전역 적용
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "Bearer Auth";

    @Value("${swagger.server-url:https://dev-api.unlock-official.app}")
    private String serverUrl;

    @Value("${swagger.server-description:Development Server}")
    private String serverDescription;

    @Bean
    public OpenAPI unlockOpenAPI() {
        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription(serverDescription);

        // JWT 인증 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("로그인 후 발급받은 AccessToken을 입력해 주세요.");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(BEARER_AUTH);

        return new OpenAPI()
                .servers(List.of(server))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes(BEARER_AUTH, securityScheme))
                .info(new Info()
                        .title("un:lock API (" + serverDescription + ")")
                        .description("우리만의 은밀한 대화, un:lock 서비스 API 문서입니다.")
                        .version("v0.0.1"));
    }
}
