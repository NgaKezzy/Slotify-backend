package com.slotify.module.salon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * Booking and payment rules of a salon (read and write shape are identical).
 *
 * @param slotIntervalMin granularity of start times (5-120)
 * @param minAdvanceBookingMin earliest start relative to now
 * @param maxAdvanceDays booking horizon
 * @param cancelBeforeMin free cancellation deadline before start
 * @param autoConfirm confirm bookings automatically
 * @param requireDeposit require an online deposit to hold the slot
 * @param depositPercent deposit as percent of the total
 * @param acceptStripe accept card payments through Stripe
 * @param acceptPaypal accept PayPal
 * @param acceptCash accept payment at the salon
 */
public record SalonSettingsDto(
    @Min(5) @Max(120) int slotIntervalMin,
    @Min(0) @Max(10080) int minAdvanceBookingMin,
    @Min(1) @Max(365) int maxAdvanceDays,
    @Min(0) @Max(43200) int cancelBeforeMin,
    boolean autoConfirm,
    boolean requireDeposit,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal depositPercent,
    boolean acceptStripe,
    boolean acceptPaypal,
    boolean acceptCash) {}
