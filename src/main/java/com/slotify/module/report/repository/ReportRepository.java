package com.slotify.module.report.repository;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.PaymentMethod;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregation queries over {@link Booking} for the report endpoints.
 *
 * <p>Every method takes a half-open UTC window {@code [from, to)} that the service derives from the
 * salon-local period. Aggregates run in the database; only the series endpoints load the period's
 * bookings once per request (see {@link #findInPeriod}), which is acceptable for v1 volumes.
 */
public interface ReportRepository extends Repository<Booking, Long> {

  /** Bookings per status starting inside the window. */
  @Query(
      """
      SELECT new com.slotify.module.report.repository.StatusCount(b.status, COUNT(b))
      FROM Booking b
      WHERE b.salon.id = :salonId AND b.startAt >= :from AND b.startAt < :to
      GROUP BY b.status
      """)
  List<StatusCount> countByStatus(
      @Param("salonId") Long salonId, @Param("from") Instant from, @Param("to") Instant to);

  /** Sum of {@code total_minor} of bookings in the given status starting inside the window. */
  @Query(
      """
      SELECT COALESCE(SUM(b.totalMinor), 0) FROM Booking b
      WHERE b.salon.id = :salonId AND b.status = :status
        AND b.startAt >= :from AND b.startAt < :to
      """)
  long sumRevenue(
      @Param("salonId") Long salonId,
      @Param("status") BookingStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Customers whose earliest booking at the salon starts inside the window. */
  @Query(
      """
      SELECT COUNT(DISTINCT b.customer.id) FROM Booking b
      WHERE b.salon.id = :salonId AND b.startAt >= :from AND b.startAt < :to
        AND NOT EXISTS (
          SELECT 1 FROM Booking earlier
          WHERE earlier.salon.id = :salonId AND earlier.customer.id = b.customer.id
            AND earlier.startAt < :from)
      """)
  long countNewCustomers(
      @Param("salonId") Long salonId, @Param("from") Instant from, @Param("to") Instant to);

  /** Calendar footprints of assigned bookings overlapping the window. */
  @Query(
      """
      SELECT new com.slotify.module.report.repository.BookedInterval(
          b.staff.id, b.startAt, b.endAt)
      FROM Booking b
      WHERE b.salon.id = :salonId AND b.staff IS NOT NULL AND b.status IN :statuses
        AND b.startAt < :to AND b.endAt > :from
      """)
  List<BookedInterval> findBookedIntervals(
      @Param("salonId") Long salonId,
      @Param("statuses") Collection<BookingStatus> statuses,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Bookings starting inside the window with items, staff and customer pre-fetched. */
  @Query(
      """
      SELECT DISTINCT b FROM Booking b
      LEFT JOIN FETCH b.items LEFT JOIN FETCH b.staff JOIN FETCH b.customer
      WHERE b.salon.id = :salonId AND b.startAt >= :from AND b.startAt < :to
      ORDER BY b.startAt ASC
      """)
  List<Booking> findInPeriod(
      @Param("salonId") Long salonId, @Param("from") Instant from, @Param("to") Instant to);

  /** Open bookings starting after the given instant. */
  long countBySalonIdAndStatusInAndStartAtGreaterThanEqual(
      Long salonId, Collection<BookingStatus> statuses, Instant from);

  // ---- platform-wide ----------------------------------------------------------------------

  /** Bookings of every salon starting inside the window. */
  long countByStartAtGreaterThanEqualAndStartAtLessThan(Instant from, Instant to);

  /** Revenue of bookings in the given status per currency, platform-wide. */
  @Query(
      """
      SELECT new com.slotify.module.report.repository.CurrencyRevenueRow(
          b.currency, COALESCE(SUM(b.totalMinor), 0), COUNT(b))
      FROM Booking b
      WHERE b.status = :status AND b.startAt >= :from AND b.startAt < :to
      GROUP BY b.currency ORDER BY b.currency
      """)
  List<CurrencyRevenueRow> revenueByCurrency(
      @Param("status") BookingStatus status, @Param("from") Instant from, @Param("to") Instant to);

  /** Salons ordered by revenue of bookings in the given status, best first. */
  @Query(
      """
      SELECT new com.slotify.module.report.repository.SalonRevenueRow(
          s.id, s.name, s.currency, COALESCE(SUM(b.totalMinor), 0), COUNT(b))
      FROM Booking b JOIN b.salon s
      WHERE b.status = :status AND b.startAt >= :from AND b.startAt < :to
      GROUP BY s.id, s.name, s.currency
      ORDER BY SUM(b.totalMinor) DESC
      """)
  List<SalonRevenueRow> topSalonsByRevenue(
      @Param("status") BookingStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Limit limit);

  /** Revenue per salon of bookings in the given status paid with the given method. */
  @Query(
      """
      SELECT new com.slotify.module.report.repository.SalonRevenueRow(
          s.id, s.name, s.currency, COALESCE(SUM(b.totalMinor), 0), COUNT(b))
      FROM Booking b JOIN b.salon s
      WHERE b.status = :status AND b.paymentMethod = :method
        AND b.startAt >= :from AND b.startAt < :to
      GROUP BY s.id, s.name, s.currency
      """)
  List<SalonRevenueRow> revenueBySalonAndMethod(
      @Param("status") BookingStatus status,
      @Param("method") PaymentMethod method,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
