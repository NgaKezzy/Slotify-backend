package com.slotify.config;

import com.slotify.security.JsonAuthErrorHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security configuration.
 *
 * <p>The API is stateless: no sessions, no CSRF, authentication via JWT bearer tokens (the JWT
 * filter is added in the auth module). Public endpoints are listed in {@link #PUBLIC_PATHS}; every
 * other request requires an authenticated user. Fine-grained rules live on the controllers via
 * {@code @PreAuthorize}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  /** Paths that never require authentication. */
  public static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/**",
    "/api/v1/system/**",
    "/api/v1/categories/**",
    "/api/v1/salons/**",
    "/api/v1/webhooks/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/actuator/health/**",
    "/ws/**"
  };

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JsonAuthErrorHandlers errorHandlers)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(errorHandlers).accessDeniedHandler(errorHandlers))
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
