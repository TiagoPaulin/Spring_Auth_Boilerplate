package com.boilerplate.springauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI () {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("Spring Auth Boilerplate API")
                .version("1.0.0")
                .description("A production-ready security boilerplate featuring Spring Boot 3, Stateless JWT Authentication, and Persistent Refresh Token management for secure session revocation.")
                .contact(new io.swagger.v3.oas.models.info.Contact()
                    .name("Tiago Paulin")
                    .email("tiagopaulin.dev@gmail.com")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(
                new Components().addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );

    }
    
}
