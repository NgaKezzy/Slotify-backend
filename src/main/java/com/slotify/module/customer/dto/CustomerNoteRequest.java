package com.slotify.module.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST .../customers/{customerId}/notes}.
 *
 * @param note free text, internal to the salon
 */
public record CustomerNoteRequest(@NotBlank @Size(max = 2000) String note) {}
