package com.slotify.module.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Body of {@code PUT /admin/salons/{salonId}/staff/{staffId}/services}: the complete set of
 * services the staff member can perform (replaces the previous set).
 *
 * @param serviceIds ids of services belonging to the same salon; may be empty
 */
public record StaffServicesRequest(@NotNull Set<Long> serviceIds) {}
