package com.slotify.module.user.entity;

import com.slotify.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * A user account of any role (see {@link Role}).
 *
 * <p>Salon staff are additionally represented by a {@code Staff} row linked through {@code
 * staffs.user_id}; customers and owners are plain users.
 */
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

  @Column(nullable = false, length = 255)
  private String email;

  @Column(length = 32)
  private String phone;

  /** BCrypt hash; {@code null} for social-login accounts. */
  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.CUSTOMER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider = AuthProvider.LOCAL;

  @Column(name = "provider_id")
  private String providerId;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(nullable = false, length = 10)
  private String locale = "en";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status = UserStatus.ACTIVE;

  /** Creates a local (email + password) account. */
  public static User local(String email, String passwordHash, String fullName, Role role) {
    User user = new User();
    user.email = email.toLowerCase();
    user.passwordHash = passwordHash;
    user.fullName = fullName;
    user.role = role;
    user.provider = AuthProvider.LOCAL;
    return user;
  }

  /** Creates a Google Sign-In account; the email is trusted as verified by Google. */
  public static User google(String email, String googleSubject, String fullName, String avatarUrl) {
    User user = new User();
    user.email = email.toLowerCase();
    user.fullName = fullName;
    user.avatarUrl = avatarUrl;
    user.role = Role.CUSTOMER;
    user.provider = AuthProvider.GOOGLE;
    user.providerId = googleSubject;
    user.emailVerified = true;
    return user;
  }

  /** Whether the account can sign in at all. */
  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  /** Whether the account has a password (local accounts only). */
  public boolean hasPassword() {
    return passwordHash != null;
  }
}
