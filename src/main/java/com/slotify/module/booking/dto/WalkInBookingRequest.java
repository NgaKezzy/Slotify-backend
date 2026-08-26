package com.slotify.module.booking.dto;

import com.slotify.module.booking.entity.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Body of {@code POST /admin/salons/{salonId}/bookings} (owner creates a booking for a customer who
 * called or walked in).
 *
 * @param customerId existing customer; or null to create/find one by {@code customerEmail}
 * @param customerName name used when creating a new customer record
 * @param customerEmail email used to find or create the customer; null creates a guest record
 * @param serviceIds services
 * @param staffId staff member (required for walk-ins)
 * @param startAt start in UTC; the owner may book outside the customer rules but not overlapping
 * @param paymentMethod expected payment method (defaults to CASH)
 * @param note internal note
 */
public record WalkInBookingRequest(
    Long customerId,
    @Size(max = 150) String customerName,
    @Email String customerEmail,
    @NotEmpty @Size(max = 10) List<Long> serviceIds,
    @NotNull Long staffId,
    @NotNull Instant startAt,
    PaymentMethod paymentMethod,
    @Size(max = 500) String note) {}
