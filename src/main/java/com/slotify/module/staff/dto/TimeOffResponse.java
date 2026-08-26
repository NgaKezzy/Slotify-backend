package com.slotify.module.staff.dto;

import com.slotify.module.staff.entity.TimeOffStatus;
import java.time.Instant;

/**
 * A stored time-off entry.
 *
 * @param id time-off id
 * @param startAt start of the absence (UTC)
 * @param endAt end of the absence (UTC)
 * @param reason optional reason
 * @param status approval state
 */
public record TimeOffResponse(
    Long id, Instant startAt, Instant endAt, String reason, TimeOffStatus status) {}
