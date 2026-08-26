package com.slotify.security;

import com.slotify.common.api.ApiError;
import com.slotify.common.api.ApiResponse;
import com.slotify.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the standard {@link ApiResponse} error envelope for security failures.
 *
 * <p>Spring Security rejects unauthenticated / unauthorised requests inside the filter chain,
 * before any controller (and therefore before {@code GlobalExceptionHandler}) runs. This component
 * makes those responses look exactly like every other API error so clients only need one error
 * parser.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  /** Called when a request has no (valid) credentials: HTTP 401. */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    write(request, response, ErrorCode.UNAUTHORIZED);
  }

  /** Called when an authenticated user lacks the required role: HTTP 403. */
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(request, response, ErrorCode.FORBIDDEN);
  }

  private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
      throws IOException {
    Locale locale = request.getLocale();
    String message = messageSource.getMessage(code.messageKey(), null, code.name(), locale);
    ApiResponse<Void> body = ApiResponse.error(message, List.of(ApiError.of(code.name(), message)));

    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
