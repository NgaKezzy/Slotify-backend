package com.slotify.module.customer.repository;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.customer.dto.CustomerStatsRow;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only CRM queries over {@code bookings}: every user with at least one booking at a salon is a
 * "customer" of that salon, and their statistics are aggregated per customer.
 *
 * <p>The status parameters exist only because HQL cannot reference the enum constants inline in a
 * portable way; callers pass {@code COMPLETED}, {@code NO_SHOW} and {@code CANCELLED}.
 */
public interface CustomerStatsRepository extends Repository<Booking, Long> {

  String SELECT =
      """
      SELECT new com.slotify.module.customer.dto.CustomerStatsRow(
        c.id, c.fullName, c.email, c.phone, c.avatarUrl,
        COUNT(b),
        SUM(CASE WHEN b.status = :completed THEN 1L ELSE 0L END),
        SUM(CASE WHEN b.status = :noShow THEN 1L ELSE 0L END),
        SUM(CASE WHEN b.status = :cancelled THEN 1L ELSE 0L END),
        SUM(CASE WHEN b.status = :completed THEN b.totalMinor ELSE 0L END),
        MIN(CASE WHEN b.status = :completed THEN b.startAt END),
        MAX(CASE WHEN b.status = :completed THEN b.startAt END))
      FROM Booking b JOIN b.customer c
      WHERE b.salon.id = :salonId
      """;

  String SEARCH = " AND (LOWER(c.fullName) LIKE :q OR LOWER(c.email) LIKE :q)";

  String GROUP =
      " GROUP BY c.id, c.fullName, c.email, c.phone, c.avatarUrl ORDER BY MAX(b.startAt) DESC";

  String COUNT =
      "SELECT COUNT(DISTINCT c.id) FROM Booking b JOIN b.customer c WHERE b.salon.id = :salonId";

  /** All customers of the salon, most recently booked first. */
  @Query(value = SELECT + GROUP, countQuery = COUNT)
  Page<CustomerStatsRow> findAll(
      @Param("salonId") Long salonId,
      @Param("completed") BookingStatus completed,
      @Param("noShow") BookingStatus noShow,
      @Param("cancelled") BookingStatus cancelled,
      Pageable pageable);

  /** Customers whose name or email matches the lower-cased LIKE pattern {@code q}. */
  @Query(value = SELECT + SEARCH + GROUP, countQuery = COUNT + SEARCH)
  Page<CustomerStatsRow> search(
      @Param("salonId") Long salonId,
      @Param("q") String q,
      @Param("completed") BookingStatus completed,
      @Param("noShow") BookingStatus noShow,
      @Param("cancelled") BookingStatus cancelled,
      Pageable pageable);

  /** Statistics of one customer; empty when the user never booked at the salon. */
  @Query(SELECT + " AND c.id = :customerId" + GROUP)
  Optional<CustomerStatsRow> findOne(
      @Param("salonId") Long salonId,
      @Param("customerId") Long customerId,
      @Param("completed") BookingStatus completed,
      @Param("noShow") BookingStatus noShow,
      @Param("cancelled") BookingStatus cancelled);
}
