package com.slotify.module.staff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Body for creating a time-off entry (owner: {@code POST
 * /admin/salons/{salonId}/staff/{staffId}/time-off}, staff: {@code POST /staff/time-off}).
 *
 * @param startAt start of the absence (UTC)
 * @param endAt end of the absence (UTC), must be after {@code startAt}
 * @param reason optional free-text reason
 */
public record TimeOffRequest(
    @NotNull Instant startAt, @NotNull Instant endAt, @Size(max = 255) String reason) {}
