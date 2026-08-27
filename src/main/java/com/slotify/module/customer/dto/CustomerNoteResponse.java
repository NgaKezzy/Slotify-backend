package com.slotify.module.customer.dto;

import java.time.Instant;

/**
 * An internal note about a customer.
 *
 * @param id note id
 * @param note text
 * @param authorId user who wrote it
 * @param authorName display name of the author
 * @param createdAt when it was written
 */
public record CustomerNoteResponse(
    Long id, String note, Long authorId, String authorName, Instant createdAt) {}
