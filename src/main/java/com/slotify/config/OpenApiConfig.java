package com.slotify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI documentation setup.
 *
 * <p>Swagger UI is served at {@code /swagger-ui.html} and the raw spec at {@code /v3/api-docs}. The
 * admin panel generates its TypeScript client from that spec ({@code pnpm gen:api}).
 */
@Configuration
public class OpenApiConfig {

  /** Name of the bearer-token security scheme referenced by secured endpoints. */
  public static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  OpenAPI slotifyOpenApi(AppProperties properties) {
    return new OpenAPI()
        .info(
            new Info()
                .title(properties.name() + " API")
                .version("v1")
                .description(
                    "REST API for the Slotify salon & spa booking platform. "
                        + "Authenticate with POST /api/v1/auth/login and send the access token "
                        + "as `Authorization: Bearer <token>`.")
                .license(new License().name("Commercial - CodeCanyon")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
