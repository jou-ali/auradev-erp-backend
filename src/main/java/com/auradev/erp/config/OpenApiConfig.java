package com.auradev.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 specification configuration.
 *
 * <p>Adds a global {@code BearerAuth} security requirement so that every
 * operation in the Swagger UI shows the lock icon and the "Authorize" button
 * can be used to set a token for all requests.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AuraDev Commerce ERP API")
                        .version("v1")
                        .description("Multi-tenant supermarket ERP — POS, Inventory, Purchases, GST billing. " +
                                "All endpoints (except /auth/login and /auth/refresh) require a valid " +
                                "Bearer JWT obtained from POST /api/v1/auth/login.")
                        .contact(new Contact()
                                .name("AuraDev Engineering")
                                .email("engineering@auradev.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide the JWT access token obtained from the /auth/login endpoint.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
