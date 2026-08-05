package com.academy.trafficviolationsystem.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Adds a JWT Bearer security scheme so the "Authorize" button in Swagger UI
 * sends the Authorization: Bearer <token> header on every secured request.
 *
 * Access Swagger UI at:  http://localhost:8080/swagger-ui/index.html
 * Access raw OpenAPI at: http://localhost:8080/v3/api-docs
 *
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Traffic Violation System API")
                        .version("v1.0")
                        .description("""
                                REST API for the Traffic Violation Management System.
                                
                                Supports:
                                  • Automatic violation detection via MQTT (cameras / radars)
                                  • Manual violation recording by traffic officers
                                  • Fine issuance with PDF generation
                                  • Payment simulation with receipt PDF
                                  • Violation appeals workflow
                                  • Heatmap analytics for accident hotspots
                                  • SMS / email notification pipeline
                                
                                Authentication: JWT Bearer token.
                                Obtain a token from POST /api/auth/login, then click Authorize below.
                                """)
                        .contact(new Contact()
                                .name("Traffic System Team")
                                .email("dev@trafficsystem.ba"))
                )
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token (without 'Bearer ' prefix)")
                        )
                );
    }
}
