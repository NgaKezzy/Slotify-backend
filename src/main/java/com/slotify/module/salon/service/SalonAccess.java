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
 * Multi-tenant guard for {@code /admin/salons/{salonId}/...} endpoints: loads a salon and checks
 * that the caller may manage it.
 *
 * <p>A {@code SALON_OWNER} may only touch salons they own; a {@code SUPER_ADMIN} may touch any
 * salon. Used by every owner-facing service (staff, services, bookings, …).
 */
@Component
@RequiredArgsConstructor
public class SalonAccess {

  private final SalonRepository salonRepository;

  /**
   * Loads the salon and asserts the caller can manage it.
   *
   * @throws AppException {@link ErrorCode#SALON_NOT_FOUND} when the salon does not exist, {@link
   *     ErrorCode#FORBIDDEN} when the caller is neither the owner nor a super admin
   */
  @Transactional(readOnly = true)
  public Salon requireOwned(Long salonId, UserPrincipal principal) {
    Salon salon =
        salonRepository
            .findById(salonId)
            .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    if (!principal.hasRole(Role.SUPER_ADMIN) && !salon.isOwnedBy(principal.id())) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return salon;
  }
}
