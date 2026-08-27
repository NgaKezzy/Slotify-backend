package com.slotify.module.user.dto;

import com.slotify.module.user.entity.AuthProvider;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.UserStatus;
import java.time.Instant;

/**
 * User account as seen by the super admin (includes status and creation time).
 *
 * @param id user id
 * @param fullName display name
 * @param email email address
 * @param phone optional contact phone
 * @param role platform role
 * @param status account status
 * @param provider how the account signs in
 * @param emailVerified whether the email has been verified
 * @param createdAt registration time
 */
public record PlatformUserResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    Role role,
    UserStatus status,
    AuthProvider provider,
    boolean emailVerified,
    Instant createdAt) {}
