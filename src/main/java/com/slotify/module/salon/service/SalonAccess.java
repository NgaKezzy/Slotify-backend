package com.slotify.module.salon.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Multi-tenant guard: resolves a salon and checks that the caller may manage it.
 *
 * <p>Use in every owner-facing service instead of loading salons directly, so the "an owner only
 * sees their own salons" rule lives in one place. {@code SUPER_ADMIN} may manage any salon.
 */
@Component
@RequiredArgsConstructor
public class SalonAccess {

  private final SalonRepository salonRepository;

  /**
   * Loads the salon and asserts ownership.
   *
   * @throws AppException {@link ErrorCode#SALON_NOT_FOUND} or {@link ErrorCode#FORBIDDEN}
   */
  @Transactional(readOnly = true)
  public Salon requireOwned(Long salonId, UserPrincipal principal) {
    Salon salon =
        salonRepository
            .findById(salonId)
            .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    if (!canManage(salon, principal)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return salon;
  }

  /** Whether the principal may manage the salon (owner or super admin). */
  public boolean canManage(Salon salon, UserPrincipal principal) {
    return principal.hasRole(Role.SUPER_ADMIN) || salon.isOwnedBy(principal.id());
  }
}
