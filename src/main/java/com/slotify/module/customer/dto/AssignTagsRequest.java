package com.slotify.module.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Body of {@code PUT .../customers/{customerId}/tags}: the complete new set of tags.
 *
 * @param tagIds ids of tags of this salon; an empty list removes every tag
 */
public record AssignTagsRequest(@NotNull @Size(max = 50) List<Long> tagIds) {}
