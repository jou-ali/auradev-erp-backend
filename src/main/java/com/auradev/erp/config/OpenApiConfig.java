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
                        .title("AuraDev ERP API")
                        .version("v1")
                        .description("AuraDev ERP — auth, products, categories, and inventory. " +
                                "Obtain a JWT from POST /api/v1/auth/login, then use Authorize in Swagger UI.")
                        .contact(new Contact()
                                .name("AuraDev Engineering")
                                .email("engineering@auradev.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken value from POST /api/v1/auth/login (without the 'Bearer ' prefix).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
