package com.slotify.module.promotion.repository;

import com.slotify.module.promotion.entity.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Coupon}. */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

  Optional<Coupon> findBySalonIdAndCode(Long salonId, String code);

  Optional<Coupon> findByIdAndSalonId(Long id, Long salonId);

  List<Coupon> findAllBySalonIdOrderByCreatedAtDesc(Long salonId);

  boolean existsBySalonIdAndCode(Long salonId, String code);
}
