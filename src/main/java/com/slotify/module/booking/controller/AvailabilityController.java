package com.slotify.module.booking.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.booking.dto.AvailabilityResponse;
import com.slotify.module.booking.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public availability lookup used by the booking flow. */
@Tag(name = "Bookings", description = "Availability and bookings")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/salons/{idOrSlug}/availability")
@RequiredArgsConstructor
@Validated
public class AvailabilityController {

  private final AvailabilityService availabilityService;

  @Operation(
      summary = "Bookable start times for a day",
      description =
          "Returns slots where every requested service fits back to back. Pass staffId to restrict "
              + "to one staff member; otherwise slots of all qualified staff are merged.")
  @GetMapping
  public ApiResponse<AvailabilityResponse> availability(
      @PathVariable String idOrSlug,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam @NotEmpty @Size(max = 10) List<Long> serviceIds,
      @RequestParam(required = false) Long staffId) {
    return ApiResponse.ok(availabilityService.availability(idOrSlug, date, serviceIds, staffId));
  }
}
