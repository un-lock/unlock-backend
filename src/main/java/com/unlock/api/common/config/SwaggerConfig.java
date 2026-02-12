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
 * - 개발 전용 도메인 고정
 * - JWT(Bearer) 인증 전역 적용
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "Bearer Auth";

    @Bean
    public OpenAPI unlockOpenAPI() {
        // [개발 전용 고정]: Swagger 호출 주소를 개발 도메인으로 단일화합니다.
        Server devServer = new Server();
        devServer.setUrl("https://dev-api.unlock-official.app");
        devServer.setDescription("Development Server");

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
                .servers(List.of(devServer))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes(BEARER_AUTH, securityScheme))
                .info(new Info()
                        .title("un:lock API (Dev)")
                        .description("우리만의 은밀한 대화, un:lock 서비스의 개발 전용 API 문서입니다.")
                        .version("v0.0.1"));
    }
}
