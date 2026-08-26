package com.slotify.module.salon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Body of {@code POST /admin/uploads/presign}.
 *
 * @param fileName original file name (extension is kept)
 * @param contentType image MIME type
 * @param folder logical folder: salons, services, avatars
 */
public record PresignRequest(
    @NotBlank String fileName,
    @NotBlank @Pattern(regexp = "^image/(jpeg|png|webp)$", message = "must be a JPEG, PNG or WebP")
        String contentType,
    @NotBlank @Pattern(regexp = "^(salons|services|avatars)$") String folder) {}
