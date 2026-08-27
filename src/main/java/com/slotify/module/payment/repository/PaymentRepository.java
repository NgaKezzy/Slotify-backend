package com.slotify.module.payment.repository;

import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Payment}. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByProviderRef(String providerRef);

  List<Payment> findAllByBookingIdOrderByCreatedAtAsc(Long bookingId);

  Page<Payment> findAllByBookingSalonIdOrderByCreatedAtDesc(Long salonId, Pageable pageable);

  /** Succeeded payments of a salon inside a period (revenue reports). */
  @Query(
      """
      SELECT p FROM Payment p
      WHERE p.booking.salon.id = :salonId AND p.status IN :states
        AND p.paidAt >= :from AND p.paidAt < :to
      """)
  List<Payment> findPaidInPeriod(
      @Param("salonId") Long salonId,
      @Param("states") List<PaymentState> states,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Gross amount of matching payments per salon, by {@code paidAt} inside the window. */
  @Query(
      """
      SELECT new com.slotify.module.payment.repository.SalonAmount(
          p.booking.salon.id, COALESCE(SUM(p.amountMinor), 0))
      FROM Payment p
      WHERE p.status IN :states AND p.provider IN :providers
        AND p.paidAt >= :from AND p.paidAt < :to
      GROUP BY p.booking.salon.id
      """)
  List<SalonAmount> sumPaidBySalon(
      @Param("states") List<PaymentState> states,
      @Param("providers") List<PaymentProvider> providers,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
