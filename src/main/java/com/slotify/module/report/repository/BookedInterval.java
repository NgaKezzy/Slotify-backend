package com.slotify.module.report.repository;

import java.time.Instant;

/**
 * Lightweight projection of a booking's calendar footprint, used for occupancy figures without
 * loading whole entities.
 *
 * @param staffId assigned staff id
 * @param startAt start (UTC)
 * @param endAt end (UTC)
 */
public record BookedInterval(Long staffId, Instant startAt, Instant endAt) {}
