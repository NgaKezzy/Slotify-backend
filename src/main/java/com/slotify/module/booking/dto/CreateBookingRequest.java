package com.slotify.module.booking.dto;

import com.slotify.module.booking.entity.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Body of {@code POST /bookings} (customer).
 *
 * @param salonId salon to book
 * @param serviceIds services performed back to back by the same staff member (1-10)
 * @param staffId preferred staff member; null lets the salon assign anyone available
 * @param startAt slot start in UTC, must be one of the slots returned by the availability endpoint
 * @param paymentMethod how the customer wants to pay
 * @param note optional note to the salon
 * @param couponCode optional promotion code (applied in Phase 2)
 */
public record CreateBookingRequest(
    @NotNull Long salonId,
    @NotEmpty @Size(max = 10) List<Long> serviceIds,
    Long staffId,
    @NotNull Instant startAt,
    @NotNull PaymentMethod paymentMethod,
    @Size(max = 500) String note,
    @Size(max = 50) String couponCode) {}
