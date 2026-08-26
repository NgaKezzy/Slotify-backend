package com.slotify.module.staff.dto;

import java.util.List;

/**
 * Payload of {@code GET /staff/shifts}: the weekly schedule plus upcoming per-date exceptions.
 *
 * @param shifts weekly working windows ordered by weekday and start time
 * @param upcomingOverrides exceptions from today onwards, ordered by date
 */
public record StaffScheduleResponse(
    List<ShiftResponse> shifts, List<ShiftOverrideResponse> upcomingOverrides) {}
