package com.slotify.module.system;

import com.slotify.common.api.ApiResponse;
import com.slotify.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated endpoints describing the running instance.
 *
 * <p>Clients call {@code GET /api/v1/system/info} on startup to verify connectivity and to read the
 * platform name. Also serves as the reference example of the {@link ApiResponse} envelope.
 */
@Tag(name = "System", description = "Instance information")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

  private final AppProperties properties;
  private final Clock clock;

  /**
   * Instance information.
   *
   * @param name platform display name
   * @param apiVersion API version prefix
   * @param serverTime current server time in UTC
   */
  public record SystemInfo(String name, String apiVersion, Instant serverTime) {}

  @Operation(summary = "Get instance information")
  @SecurityRequirements
  @GetMapping("/info")
  public ApiResponse<SystemInfo> info() {
    return ApiResponse.ok(new SystemInfo(properties.name(), "v1", Instant.now(clock)));
  }
}
