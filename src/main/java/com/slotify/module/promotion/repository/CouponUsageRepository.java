package com.slotify.module.promotion.repository;

import com.slotify.module.promotion.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link CouponUsage}. */
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

  long countByCouponIdAndUserId(Long couponId, Long userId);

  void deleteByBookingId(Long bookingId);
}
