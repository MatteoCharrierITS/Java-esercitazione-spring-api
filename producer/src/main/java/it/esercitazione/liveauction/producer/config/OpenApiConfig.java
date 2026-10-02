package it.esercitazione.liveauction.producer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI liveAuctionOpenApi() {
        return new OpenAPI()
                .info(new Info().title("LiveAuction Producer API").version("v1"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer operationSecurity() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean publicGet = method == PathItem.HttpMethod.GET && (
                    path.equals("/api/v1/health") || path.equals("/api/v1/ready")
                            || path.equals("/api/v1/prodotti") || path.startsWith("/api/v1/prodotti/")
                            || path.equals("/api/v1/aste") || path.startsWith("/api/v1/aste/"));
            boolean publicAuth = method == PathItem.HttpMethod.POST && List.of(
                    "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").contains(path);
            operation.setSecurity(publicGet || publicAuth ? List.of()
                    : List.of(new SecurityRequirement().addList("bearerAuth")));
        }));
    }
}
