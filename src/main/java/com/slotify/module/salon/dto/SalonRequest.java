package com.slotify.module.salon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Body of {@code POST /admin/salons} and {@code PUT /admin/salons/{id}} (owner-editable fields).
 *
 * @param name display name
 * @param description free text
 * @param phone contact phone
 * @param email contact email
 * @param address street address
 * @param city city
 * @param country ISO-3166-1 alpha-2 code
 * @param lat latitude
 * @param lng longitude
 * @param timezone IANA timezone id
 * @param currency ISO-4217 currency
 * @param coverUrl cover image URL
 * @param categoryIds global category ids
 * @param amenityIds global amenity ids
 */
public record SalonRequest(
    @NotBlank @Size(max = 150) String name,
    @Size(max = 5000) String description,
    @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "must be a valid phone number")
        String phone,
    @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO-3166 alpha-2 code")
        String country,
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal lat,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal lng,
    @NotBlank @Size(max = 64) String timezone,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO-4217 code") String currency,
    @Size(max = 500) String coverUrl,
    List<Long> categoryIds,
    List<Long> amenityIds) {}
