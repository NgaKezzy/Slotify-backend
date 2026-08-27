package com.slotify.module.promotion.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.promotion.dto.CouponRequest;
import com.slotify.module.promotion.dto.CouponResponse;
import com.slotify.module.promotion.entity.Coupon;
import com.slotify.module.promotion.entity.CouponUsage;
import com.slotify.module.promotion.repository.CouponRepository;
import com.slotify.module.promotion.repository.CouponUsageRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coupon management (owner) and application to bookings (called by the booking module). */
@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

  private final CouponRepository couponRepository;
  private final CouponUsageRepository usageRepository;
  private final SalonAccess salonAccess;
  private final Clock clock;

  // ---- Owner CRUD ------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<CouponResponse> list(Long salonId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    return couponRepository.findAllBySalonIdOrderByCreatedAtDesc(salonId).stream()
        .map(CouponService::toResponse)
        .toList();
  }

  public CouponResponse create(Long salonId, UserPrincipal principal, CouponRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    String code = Coupon.normalize(request.code());
    if (couponRepository.existsBySalonIdAndCode(salonId, code)) {
      throw new AppException(ErrorCode.CONFLICT);
    }
    Coupon coupon = Coupon.create(salon, code, request.type(), request.value());
    apply(coupon, request);
    return toResponse(couponRepository.save(coupon));
  }

  public CouponResponse update(
      Long salonId, Long couponId, UserPrincipal principal, CouponRequest request) {
    salonAccess.requireOwned(salonId, principal);
    Coupon coupon = require(salonId, couponId);
    String code = Coupon.normalize(request.code());
    if (!coupon.getCode().equals(code) && couponRepository.existsBySalonIdAndCode(salonId, code)) {
      throw new AppException(ErrorCode.CONFLICT);
    }
    coupon.setCode(code);
    coupon.setType(request.type());
    coupon.setValue(request.value());
    apply(coupon, request);
    return toResponse(coupon);
  }

  public void delete(Long salonId, Long couponId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    couponRepository.delete(require(salonId, couponId));
  }

  // ---- Application -----------------------------------------------------------------------------

  /**
   * Resolves a usable coupon for the customer or throws {@link ErrorCode#COUPON_INVALID}.
   *
   * @return the coupon and the discount for the subtotal
   */
  @Transactional(readOnly = true)
  public Applied resolve(Salon salon, String code, Long userId, long subtotalMinor) {
    Coupon coupon =
        couponRepository
            .findBySalonIdAndCode(salon.getId(), Coupon.normalize(code))
            .orElseThrow(() -> new AppException(ErrorCode.COUPON_INVALID));
    Instant now = Instant.now(clock);
    boolean perUserOk =
        coupon.getPerUserLimit() == null
            || usageRepository.countByCouponIdAndUserId(coupon.getId(), userId)
                < coupon.getPerUserLimit();
    if (!coupon.isUsable(now) || !perUserOk || subtotalMinor < coupon.getMinOrderMinor()) {
      throw new AppException(ErrorCode.COUPON_INVALID);
    }
    return new Applied(coupon, coupon.discountFor(subtotalMinor));
  }

  /** Records the usage once the booking exists. */
  public void recordUsage(Coupon coupon, Long userId, Long bookingId) {
    coupon.setUsedCount(coupon.getUsedCount() + 1);
    usageRepository.save(CouponUsage.of(coupon, userId, bookingId));
  }

  /** Optional lookup used when re-validating an existing booking. */
  @Transactional(readOnly = true)
  public Optional<Coupon> find(Long couponId) {
    return couponId == null ? Optional.empty() : couponRepository.findById(couponId);
  }

  /**
   * A coupon resolved for a booking.
   *
   * @param coupon the coupon
   * @param discountMinor discount to subtract from the subtotal
   */
  public record Applied(Coupon coupon, long discountMinor) {}

  // ---- helpers ---------------------------------------------------------------------------------

  private Coupon require(Long salonId, Long couponId) {
    return couponRepository
        .findByIdAndSalonId(couponId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
  }

  private static void apply(Coupon coupon, CouponRequest request) {
    if (request.type() == com.slotify.module.promotion.entity.CouponType.PERCENT
        && request.value() > 100) {
      throw new AppException(ErrorCode.VALIDATION_FAILED);
    }
    coupon.setMinOrderMinor(request.minOrderMinor());
    coupon.setMaxDiscountMinor(request.maxDiscountMinor());
    coupon.setUsageLimit(request.usageLimit());
    coupon.setPerUserLimit(request.perUserLimit());
    coupon.setStartsAt(request.startsAt());
    coupon.setEndsAt(request.endsAt());
    coupon.setActive(request.active());
  }

  static CouponResponse toResponse(Coupon c) {
    return new CouponResponse(
        c.getId(),
        c.getCode(),
        c.getType(),
        c.getValue(),
        c.getMinOrderMinor(),
        c.getMaxDiscountMinor(),
        c.getUsageLimit(),
        c.getUsedCount(),
        c.getPerUserLimit(),
        c.getStartsAt(),
        c.getEndsAt(),
        c.isActive());
  }
}
