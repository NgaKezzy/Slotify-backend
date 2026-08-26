package com.slotify.module.salon.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Booking and payment rules of a salon (one row per salon). */
@Entity
@Table(name = "salon_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalonSettings extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  /** Granularity of bookable start times, in minutes. */
  @Column(name = "slot_interval_min", nullable = false)
  private int slotIntervalMin = 15;

  /** Earliest bookable start relative to now, in minutes. */
  @Column(name = "min_advance_booking_min", nullable = false)
  private int minAdvanceBookingMin = 60;

  /** How far into the future customers may book, in days. */
  @Column(name = "max_advance_days", nullable = false)
  private int maxAdvanceDays = 60;

  /** Customers may cancel until this many minutes before the start. */
  @Column(name = "cancel_before_min", nullable = false)
  private int cancelBeforeMin = 1440;

  /** When true new bookings are CONFIRMED immediately instead of PENDING. */
  @Column(name = "auto_confirm", nullable = false)
  private boolean autoConfirm = true;

  @Column(name = "require_deposit", nullable = false)
  private boolean requireDeposit;

  @Column(name = "deposit_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal depositPercent = BigDecimal.ZERO;

  @Column(name = "accept_stripe", nullable = false)
  private boolean acceptStripe = true;

  @Column(name = "accept_paypal", nullable = false)
  private boolean acceptPaypal;

  @Column(name = "accept_cash", nullable = false)
  private boolean acceptCash = true;

  static SalonSettings defaults(Salon salon) {
    SalonSettings settings = new SalonSettings();
    settings.salon = salon;
    return settings;
  }
}
