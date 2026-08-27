package com.slotify.config;

import com.slotify.security.JsonAuthErrorHandlers;
import com.slotify.security.JwtAuthenticationFilter;
import com.slotify.security.RateLimitFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * HTTP security configuration.
 *
 * <p>The API is stateless: no sessions, no CSRF, authentication via JWT bearer tokens ({@link
 * JwtAuthenticationFilter}); {@link RateLimitFilter} throttles the auth endpoints. Public endpoints
 * are listed in {@link #PUBLIC_PATHS}; every other request requires an authenticated user.
 * Fine-grained rules live on the controllers via {@code @PreAuthorize}.
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
    "/api/v1/amenities/**",
    "/api/v1/salons/**",
    "/api/v1/webhooks/**",
    "/api/v1/payments/config",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/actuator/health/**",
    "/ws/**"
  };

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JsonAuthErrorHandlers errorHandlers,
      JwtAuthenticationFilter jwtFilter,
      RateLimitFilter rateLimitFilter)
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
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
