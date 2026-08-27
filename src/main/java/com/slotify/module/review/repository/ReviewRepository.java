package com.slotify.module.review.repository;

import com.slotify.module.review.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Review}. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

  Optional<Review> findByBookingId(Long bookingId);

  boolean existsByBookingId(Long bookingId);

  Optional<Review> findByIdAndSalonId(Long id, Long salonId);

  Page<Review> findAllBySalonIdAndVisibleTrue(Long salonId, Pageable pageable);

  Page<Review> findAllBySalonId(Long salonId, Pageable pageable);

  Page<Review> findAllBySalonIdAndVisible(Long salonId, boolean visible, Pageable pageable);

  /** Average of the visible ratings of a salon ({@code null} when there are none). */
  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.salon.id = :salonId AND r.visible = true")
  Double averageForSalon(@Param("salonId") Long salonId);

  /** Number of visible reviews of a salon. */
  @Query("SELECT COUNT(r) FROM Review r WHERE r.salon.id = :salonId AND r.visible = true")
  long countVisibleForSalon(@Param("salonId") Long salonId);

  /** Average of the visible ratings of a staff member ({@code null} when there are none). */
  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.staff.id = :staffId AND r.visible = true")
  Double averageForStaff(@Param("staffId") Long staffId);

  /** Number of visible reviews of a staff member. */
  @Query("SELECT COUNT(r) FROM Review r WHERE r.staff.id = :staffId AND r.visible = true")
  long countVisibleForStaff(@Param("staffId") Long staffId);

  /** Number of visible reviews per rating value: rows of {@code [Integer rating, Long count]}. */
  @Query(
      """
      SELECT r.rating, COUNT(r) FROM Review r
      WHERE r.salon.id = :salonId AND r.visible = true
      GROUP BY r.rating
      """)
  List<Object[]> distributionForSalon(@Param("salonId") Long salonId);
}
