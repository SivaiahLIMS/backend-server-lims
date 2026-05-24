package com.sivayahealth.lims.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenApiConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public OpenAPI limsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LIMS API - Laboratory Information Management System")
                        .description("Enterprise-grade, AI-powered, multi-tenant LIMS platform")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
