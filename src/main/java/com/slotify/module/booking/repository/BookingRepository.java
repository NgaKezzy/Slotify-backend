package com.slotify.module.booking.repository;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Booking}. */
public interface BookingRepository
    extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

  boolean existsByCode(String code);

  Optional<Booking> findByIdAndSalonId(Long id, Long salonId);

  Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

  Optional<Booking> findByIdAndStaffId(Long id, Long staffId);

  /** Calendar-blocking bookings of the given staff overlapping {@code [from, to)}. */
  @Query(
      """
      SELECT b FROM Booking b
      WHERE b.staff.id IN :staffIds AND b.status IN :statuses
        AND b.startAt < :to AND b.endAt > :from
      """)
  List<Booking> findOverlapping(
      @Param("staffIds") Collection<Long> staffIds,
      @Param("statuses") Collection<BookingStatus> statuses,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Same as {@link #findOverlapping} for a single staff member but with a row lock, used inside the
   * booking transaction to make the final double-booking check race-free.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT b FROM Booking b
      WHERE b.staff.id = :staffId AND b.status IN :statuses
        AND b.startAt < :to AND b.endAt > :from
      """)
  List<Booking> findOverlappingForUpdate(
      @Param("staffId") Long staffId,
      @Param("statuses") Collection<BookingStatus> statuses,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Complete history of a customer (GDPR export). */
  List<Booking> findAllByCustomerIdOrderByStartAtDesc(Long customerId);

  Page<Booking> findAllByCustomerIdAndStatusIn(
      Long customerId, Collection<BookingStatus> statuses, Pageable pageable);

  Page<Booking> findAllByCustomerIdAndStatusNotIn(
      Long customerId, Collection<BookingStatus> statuses, Pageable pageable);

  List<Booking> findAllByStaffIdAndStartAtBetweenOrderByStartAtAsc(
      Long staffId, Instant from, Instant to);

  List<Booking> findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(
      Long salonId, Instant from, Instant to);

  /** Confirmed bookings starting inside the window that have not been reminded yet. */
  List<Booking> findAllByStatusAndStartAtBetweenAndReminderSentAtIsNull(
      BookingStatus status, Instant from, Instant to);

  /** Bookings that should have finished but were never completed by the salon. */
  List<Booking> findAllByStatusInAndEndAtBefore(Collection<BookingStatus> statuses, Instant before);

  /** Online-payment bookings still unpaid that were created before the cutoff. */
  @Query(
      """
      SELECT b FROM Booking b
      WHERE b.paymentStatus = com.slotify.module.booking.entity.PaymentStatus.UNPAID
        AND b.paymentMethod IN (com.slotify.module.booking.entity.PaymentMethod.STRIPE,
                                com.slotify.module.booking.entity.PaymentMethod.PAYPAL)
        AND b.status IN (com.slotify.module.booking.entity.BookingStatus.PENDING,
                         com.slotify.module.booking.entity.BookingStatus.CONFIRMED)
        AND b.createdAt < :cutoff
      """)
  List<Booking> findUnpaidOnlineBookingsCreatedBefore(@Param("cutoff") Instant cutoff);

  long countByStaffIdAndStatusInAndStartAtBetween(
      Long staffId, Collection<BookingStatus> statuses, Instant from, Instant to);

  /** Booking history of one customer at a salon (CRM detail); pass a small page for "last N". */
  List<Booking> findAllBySalonIdAndCustomerIdOrderByStartAtDesc(
      Long salonId, Long customerId, Pageable pageable);
}
