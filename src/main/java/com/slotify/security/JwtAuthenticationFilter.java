package com.slotify.security;

import com.slotify.common.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>} and, when valid, populates the security context with a
 * {@link UserPrincipal}.
 *
 * <p>Invalid or expired tokens are ignored here (the request simply stays anonymous) so that public
 * endpoints keep working; protected endpoints then fail with 401 through {@link
 * JsonAuthErrorHandlers}. Clients refresh on 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      authenticate(request, header.substring(BEARER_PREFIX.length()));
    }
    chain.doFilter(request, response);
  }

  private void authenticate(HttpServletRequest request, String token) {
    try {
      UserPrincipal principal = jwtService.parse(token);
      var authentication =
          new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (AppException ignored) {
      SecurityContextHolder.clearContext();
    }
  }
}
