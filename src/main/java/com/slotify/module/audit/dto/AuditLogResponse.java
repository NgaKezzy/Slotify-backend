package com.slotify.module.audit.dto;

import com.slotify.module.audit.entity.AuditAction;
import java.time.Instant;
import java.util.Map;

/**
 * One audit log line as shown in the admin panel.
 *
 * @param id log id
 * @param actorId user who performed the action ({@code null} for system events)
 * @param salonId salon the change belongs to ({@code null} for platform events)
 * @param action what happened
 * @param entity logical entity name (e.g. {@code Booking})
 * @param entityId id of the affected entity
 * @param diff changed fields as {@code {field: {from, to}}} or any other structured payload
 * @param createdAt when the change happened (UTC)
 */
public record AuditLogResponse(
    Long id,
    Long actorId,
    Long salonId,
    AuditAction action,
    String entity,
    Long entityId,
    Map<String, Object> diff,
    Instant createdAt) {}
