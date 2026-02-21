package com.jimmy.monetization.monetizationbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String TENANT_BEARER = "tenantBearer";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(TENANT_BEARER))
                .components(new Components()
                        .addSecuritySchemes(TENANT_BEARER,
                                new SecurityScheme()
                                        .name(TENANT_BEARER)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("API Key")
                        )
                );
    }
}