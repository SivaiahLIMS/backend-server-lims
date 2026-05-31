package com.sivayahealth.lims.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

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
                .servers(List.of(
                        new Server()
                                .url("https://backend-server-lims-ybsnm6cnka-el.a.run.app/api/v1")
                                .description("Production")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
