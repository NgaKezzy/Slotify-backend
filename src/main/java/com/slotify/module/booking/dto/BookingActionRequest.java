package com.slotify.module.booking.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional body of status-changing endpoints (cancel, reject, no-show).
 *
 * @param reason free-text reason shown to the other party
 */
public record BookingActionRequest(@Size(max = 255) String reason) {}
