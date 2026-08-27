package com.slotify.module.payment.repository;

import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import com.slotify.module.payment.entity.Refund;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Refund}. */
public interface RefundRepository extends JpaRepository<Refund, Long> {

  List<Refund> findAllByPaymentIdOrderByCreatedAtAsc(Long paymentId);

  @Query(
      "SELECT COALESCE(SUM(r.amountMinor), 0) FROM Refund r "
          + "WHERE r.payment.id = :paymentId AND r.status = 'SUCCEEDED'")
  long sumSucceededByPaymentId(@Param("paymentId") Long paymentId);

  /**
   * Succeeded refunds per salon of the payments matched by {@code
   * PaymentRepository#sumPaidBySalon}.
   */
  @Query(
      """
      SELECT new com.slotify.module.payment.repository.SalonAmount(
          p.booking.salon.id, COALESCE(SUM(r.amountMinor), 0))
      FROM Refund r JOIN r.payment p
      WHERE r.status = :refundStatus AND p.status IN :states AND p.provider IN :providers
        AND p.paidAt >= :from AND p.paidAt < :to
      GROUP BY p.booking.salon.id
      """)
  List<SalonAmount> sumRefundedBySalon(
      @Param("refundStatus") Refund.Status refundStatus,
      @Param("states") List<PaymentState> states,
      @Param("providers") List<PaymentProvider> providers,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
