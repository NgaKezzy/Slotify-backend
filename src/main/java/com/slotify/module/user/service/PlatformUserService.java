package com.slotify.module.user.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.auth.service.RefreshTokenService;
import com.slotify.module.user.dto.PlatformUserResponse;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.entity.UserStatus;
import com.slotify.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User administration for the super admin: search, suspend / reactivate, change role.
 *
 * <p>The platform must always keep at least one {@code SUPER_ADMIN} able to sign in, so demoting or
 * suspending the last one is refused with {@link ErrorCode#LAST_SUPER_ADMIN}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformUserService {

  /** Largest page a client may request. */
  public static final int MAX_PAGE_SIZE = 100;

  private final UserRepository userRepository;
  private final RefreshTokenService refreshTokenService;

  /** Searches accounts by email / name fragment and optional role, newest first. */
  @Transactional(readOnly = true)
  public PageResponse<PlatformUserResponse> search(String query, Role role, int page, int size) {
    String pattern =
        query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase() + "%";
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.clamp(size, 1, MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        userRepository.search(pattern, role, pageable), PlatformUserService::toResponse);
  }

  @Transactional(readOnly = true)
  public PlatformUserResponse get(Long userId) {
    return toResponse(load(userId));
  }

  /**
   * Activates or suspends an account. Suspension revokes every refresh token so open sessions end
   * as soon as their access token expires.
   *
   * @throws AppException {@link ErrorCode#BAD_REQUEST} for DELETED, {@link
   *     ErrorCode#LAST_SUPER_ADMIN} when suspending the only super admin
   */
  public PlatformUserResponse updateStatus(Long userId, UserStatus status) {
    if (status == UserStatus.DELETED) {
      throw new AppException(ErrorCode.BAD_REQUEST);
    }
    User user = load(userId);
    if (status == UserStatus.SUSPENDED && user.getStatus() != UserStatus.SUSPENDED) {
      assertNotLastSuperAdmin(user);
      refreshTokenService.revokeAll(user.getId());
    }
    user.setStatus(status);
    return toResponse(user);
  }

  /**
   * Changes the platform role.
   *
   * @throws AppException {@link ErrorCode#LAST_SUPER_ADMIN} when demoting the only super admin
   */
  public PlatformUserResponse updateRole(Long userId, Role role) {
    User user = load(userId);
    if (user.getRole() == Role.SUPER_ADMIN && role != Role.SUPER_ADMIN) {
      assertNotLastSuperAdmin(user);
    }
    user.setRole(role);
    return toResponse(user);
  }

  private void assertNotLastSuperAdmin(User user) {
    if (user.getRole() == Role.SUPER_ADMIN && userRepository.countByRole(Role.SUPER_ADMIN) <= 1) {
      throw new AppException(ErrorCode.LAST_SUPER_ADMIN);
    }
  }

  private User load(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private static PlatformUserResponse toResponse(User user) {
    return new PlatformUserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        user.getRole(),
        user.getStatus(),
        user.getProvider(),
        user.isEmailVerified(),
        user.getCreatedAt());
  }
}
