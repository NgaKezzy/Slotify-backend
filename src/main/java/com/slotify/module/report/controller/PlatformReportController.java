package com.slotify.module.report.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.report.dto.PayoutsReportResponse;
import com.slotify.module.report.dto.PlatformOverviewResponse;
import com.slotify.module.report.export.ExportType;
import com.slotify.module.report.service.PlatformReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-wide reports (SUPER_ADMIN). Dates are UTC days, both inclusive. */
@Tag(name = "Platform reports", description = "Super admin: overview and payouts")
@RestController
@RequestMapping("/api/v1/admin/platform/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformReportController {

  private final PlatformReportService platformReportService;

  @Operation(summary = "Salons by status, users by role, last 30 days bookings and revenue")
  @GetMapping("/overview")
  public ApiResponse<PlatformOverviewResponse> overview() {
    return ApiResponse.ok(platformReportService.overview());
  }

  @Operation(summary = "Online revenue, commission and payout per active salon")
  @GetMapping("/payouts")
  public ApiResponse<PayoutsReportResponse> payouts(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.ok(platformReportService.payouts(from, to));
  }

  @Operation(summary = "Download the payouts report")
  @GetMapping("/payouts/export")
  public ResponseEntity<byte[]> exportPayouts(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "EXCEL") ExportType type) {
    return SalonReportController.asAttachment(platformReportService.exportPayouts(from, to, type));
  }
}
