package com.slotify.security;

import com.slotify.module.user.entity.Role;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticated identity placed in the security context by {@link JwtAuthenticationFilter}.
 *
 * <p>Deliberately small (id, email, role) so that no database access is needed per request; load
 * the full {@code User} in the service layer when required.
 *
 * @param id user id (JWT subject)
 * @param email email address
 * @param role platform role
 */
public record UserPrincipal(Long id, String email, Role role) {

  /** Prefix Spring Security expects on role authorities ({@code hasRole("X")} → {@code ROLE_X}). */
  public static final String ROLE_PREFIX = "ROLE_";

  /** Authorities derived from the role. */
  public List<GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
  }

  public boolean hasRole(Role expected) {
    return role == expected;
  }
}
