package com.slotify.module.staff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of {@code PUT /admin/salons/{salonId}/staff/{staffId}/shifts}: the complete weekly schedule
 * (replaces the previous one). Windows on the same weekday must not overlap.
 *
 * @param shifts working windows; an empty list clears the schedule
 */
public record ReplaceShiftsRequest(@NotNull List<@Valid ShiftRequest> shifts) {}
