package com.slotify.module.payment.repository;

import com.slotify.module.payment.entity.Refund;
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
}
