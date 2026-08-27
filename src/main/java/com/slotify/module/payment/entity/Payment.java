package com.slotify.module.payment.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.booking.entity.Booking;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One payment attempt for a booking (a booking can have several: failed retry, deposit + rest).
 *
 * <p>{@code providerRef} is the Stripe PaymentIntent id / PayPal order id used to correlate
 * webhooks; {@code raw} keeps the last provider payload for support.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentProvider provider;

  @Column(name = "provider_ref")
  private String providerRef;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentType type = PaymentType.FULL;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentState status = PaymentState.PENDING;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_json")
  private Map<String, Object> raw;

  @Column(name = "paid_at")
  private Instant paidAt;

  /** Total refunded so far (derived from {@link Refund} rows on each refund). */
  @jakarta.persistence.Transient private long refundedMinor;

  public static Payment create(
      Booking booking, PaymentProvider provider, PaymentType type, long amountMinor) {
    Payment payment = new Payment();
    payment.booking = booking;
    payment.provider = provider;
    payment.type = type;
    payment.amountMinor = amountMinor;
    payment.currency = booking.getCurrency();
    return payment;
  }

  public boolean isSucceeded() {
    return status == PaymentState.SUCCEEDED || status == PaymentState.PARTIAL_REFUND;
  }
}
