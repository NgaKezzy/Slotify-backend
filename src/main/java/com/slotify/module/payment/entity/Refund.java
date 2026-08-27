package com.slotify.module.payment.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A (partial) refund of a successful payment. */
@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

  /** Outcome of the refund request at the provider. */
  public enum Status {
    PENDING,
    SUCCEEDED,
    FAILED
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "provider_ref")
  private String providerRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  @Column(length = 255)
  private String reason;

  public static Refund create(Payment payment, long amountMinor, String reason) {
    Refund refund = new Refund();
    refund.payment = payment;
    refund.amountMinor = amountMinor;
    refund.reason = reason;
    return refund;
  }
}
