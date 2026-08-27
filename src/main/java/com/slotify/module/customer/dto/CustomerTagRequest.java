package com.slotify.module.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for creating or updating a customer tag.
 *
 * @param name label, unique per salon
 * @param color hex colour like {@code #FF8800}; defaults to grey when omitted
 */
public record CustomerTagRequest(
    @NotBlank @Size(max = 50) String name,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex colour like #FF8800")
        String color) {}
