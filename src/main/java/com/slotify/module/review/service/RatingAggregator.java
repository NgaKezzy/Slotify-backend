package com.slotify.module.review.service;

import com.slotify.module.review.repository.ReviewRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.staff.entity.Staff;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the denormalised {@code salons.rating_avg / rating_count} and {@code staffs.rating_avg /
 * rating_count} columns in sync with the visible reviews.
 *
 * <p>The columns are recomputed from scratch (average + count query) instead of incrementally, so a
 * hide / unhide can never leave them drifting.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RatingAggregator {

  private static final int SCALE = 2;

  private final ReviewRepository reviewRepository;

  /** Recomputes the salon's and (when present) the staff member's rating columns. */
  public void refresh(Salon salon, Staff staff) {
    salon.setRatingAvg(average(reviewRepository.averageForSalon(salon.getId())));
    salon.setRatingCount((int) reviewRepository.countVisibleForSalon(salon.getId()));
    if (staff != null) {
      staff.setRatingAvg(average(reviewRepository.averageForStaff(staff.getId())));
      staff.setRatingCount((int) reviewRepository.countVisibleForStaff(staff.getId()));
    }
  }

  /** Rounds an average to two decimals; {@code null} (no reviews) becomes {@code 0.00}. */
  static BigDecimal average(Double avg) {
    BigDecimal value = avg == null ? BigDecimal.ZERO : BigDecimal.valueOf(avg);
    return value.setScale(SCALE, RoundingMode.HALF_UP);
  }
}
