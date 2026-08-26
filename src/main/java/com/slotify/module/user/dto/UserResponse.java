package com.slotify.module.user.dto;

import com.slotify.module.user.entity.AuthProvider;
import com.slotify.module.user.entity.Role;

/**
 * Public representation of a user account (returned by {@code /me} and auth endpoints).
 *
 * @param id user id
 * @param fullName display name
 * @param email email address (unique)
 * @param role platform role
 * @param phone optional contact phone
 * @param avatarUrl optional avatar image
 * @param locale preferred language code
 * @param emailVerified whether the email has been verified
 * @param provider how the account signs in
 */
public record UserResponse(
    Long id,
    String fullName,
    String email,
    Role role,
    String phone,
    String avatarUrl,
    String locale,
    boolean emailVerified,
    AuthProvider provider) {}
