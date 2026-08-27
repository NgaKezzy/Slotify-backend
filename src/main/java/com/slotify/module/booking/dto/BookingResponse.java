package com.slotify.module.booking.dto;

import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.CancelledBy;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.booking.entity.PaymentStatus;
import java.time.Instant;
import java.util.List;

/**
 * Booking as seen by customers, owners and staff (owners/staff additionally get customer contact
 * details; customers get {@code customer == null}).
 */
public record BookingResponse(
    Long id,
    String code,
    SalonRef salon,
    StaffRef staff,
    boolean staffAutoAssigned,
    CustomerRef customer,
    List<Item> items,
    Instant startAt,
    Instant endAt,
    BookingStatus status,
    long subtotalMinor,
    long discountMinor,
    String couponCode,
    long totalMinor,
    String currency,
    PaymentStatus paymentStatus,
    PaymentMethod paymentMethod,
    String note,
    String cancelReason,
    CancelledBy cancelledBy,
    Instant createdAt) {

  /** Minimal salon info for booking lists. */
  public record SalonRef(Long id, String name, String slug, String address, String timezone) {}

  /** Assigned staff member. */
  public record StaffRef(Long id, String displayName, String avatarUrl) {}

  /** Customer contact (owner / staff views only). */
  public record CustomerRef(Long id, String fullName, String email, String phone) {}

  /** Service line. */
  public record Item(Long serviceId, String serviceName, int durationMin, long priceMinor) {}
}
