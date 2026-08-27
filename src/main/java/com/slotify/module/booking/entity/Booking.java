package com.slotify.module.booking.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.promotion.entity.Coupon;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An appointment of one customer with one staff member for one or more services performed back to
 * back. Prices are snapshotted into {@link BookingItem}s so later menu changes do not alter
 * history.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends BaseEntity {

  @Column(nullable = false, length = 20)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "staff_id")
  private Staff staff;

  @Column(name = "staff_auto_assigned", nullable = false)
  private boolean staffAutoAssigned;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingStatus status = BookingStatus.PENDING;

  @Column(name = "subtotal_minor", nullable = false)
  private long subtotalMinor;

  @Column(name = "discount_minor", nullable = false)
  private long discountMinor;

  @Column(name = "total_minor", nullable = false)
  private long totalMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false)
  private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method")
  private PaymentMethod paymentMethod;

  @Column(length = 500)
  private String note;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "cancelled_by")
  private CancelledBy cancelledBy;

  @Column(name = "reminder_sent_at")
  private Instant reminderSentAt;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BookingItem> items = new ArrayList<>();

  /** Creates a booking; totals are computed from the items added afterwards. */
  public static Booking create(
      String code, Salon salon, User customer, Staff staff, Instant startAt, Instant endAt) {
    Booking booking = new Booking();
    booking.code = code;
    booking.salon = salon;
    booking.customer = customer;
    booking.staff = staff;
    booking.startAt = startAt;
    booking.endAt = endAt;
    booking.currency = salon.getCurrency();
    return booking;
  }

  public void addItem(BookingItem item) {
    item.setBooking(this);
    items.add(item);
    recalculateTotals();
  }

  /** Recomputes subtotal and total from items and discount. */
  public void recalculateTotals() {
    subtotalMinor = items.stream().mapToLong(BookingItem::getPriceMinor).sum();
    totalMinor = Math.max(0, subtotalMinor - discountMinor);
  }

  /** Total calendar time the booking blocks. */
  public Duration duration() {
    return Duration.between(startAt, endAt);
  }

  /**
   * Moves to a new status.
   *
   * @throws IllegalStateException when the transition is not allowed (callers check first)
   */
  public void transitionTo(BookingStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new IllegalStateException(status + " -> " + target);
    }
    status = target;
  }

  public boolean isOwnedByCustomer(Long userId) {
    return customer.getId().equals(userId);
  }
}
