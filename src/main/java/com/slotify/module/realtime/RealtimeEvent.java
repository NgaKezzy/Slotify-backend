package com.slotify.module.realtime;

import java.time.Instant;

/**
 * Envelope of every message sent over STOMP.
 *
 * @param type event name, e.g. {@code BOOKING_CREATED}
 * @param payload event-specific data (a DTO)
 * @param sentAt server time
 */
public record RealtimeEvent(String type, Object payload, Instant sentAt) {}
