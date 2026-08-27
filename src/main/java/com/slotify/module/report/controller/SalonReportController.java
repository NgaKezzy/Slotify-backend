package com.slotify.module.report.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.report.dto.BookingsReportResponse;
import com.slotify.module.report.dto.DashboardResponse;
import com.slotify.module.report.dto.Granularity;
import com.slotify.module.report.dto.RevenueReportResponse;
import com.slotify.module.report.dto.StaffPerformanceResponse;
import com.slotify.module.report.export.ExportType;
import com.slotify.module.report.export.ExportedFile;
import com.slotify.module.report.export.ReportKind;
import com.slotify.module.report.service.ReportExportService;
import com.slotify.module.report.service.ReportService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salon reports for the admin panel. {@code from}/{@code to} are ISO dates in the salon timezone,
 * both inclusive; when omitted the last 30 days are used.
 */
@Tag(name = "Reports (owner)", description = "Dashboard KPIs, revenue, bookings, staff, export")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class SalonReportController {

  private final ReportService reportService;
  private final ReportExportService exportService;

  @Operation(summary = "Dashboard KPIs versus the previous period, today's agenda")
  @GetMapping("/dashboard")
  public ApiResponse<DashboardResponse> dashboard(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.ok(reportService.dashboard(principal, salonId, from, to));
  }

  @Operation(summary = "Revenue series with payment-method and service breakdowns")
  @GetMapping("/revenue")
  public ApiResponse<RevenueReportResponse> revenue(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "DAY") Granularity granularity) {
    return ApiResponse.ok(reportService.revenue(principal, salonId, from, to, granularity));
  }

  @Operation(summary = "Booking counts by status over time")
  @GetMapping("/bookings")
  public ApiResponse<BookingsReportResponse> bookings(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "DAY") Granularity granularity) {
    return ApiResponse.ok(reportService.bookings(principal, salonId, from, to, granularity));
  }

  @Operation(summary = "Per-staff bookings, revenue, rating and utilisation")
  @GetMapping("/staff-performance")
  public ApiResponse<StaffPerformanceResponse> staffPerformance(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.ok(reportService.staffPerformance(principal, salonId, from, to));
  }

  @Operation(summary = "Download a report as Excel or PDF")
  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam ExportType type,
      @RequestParam ReportKind report) {
    ExportedFile file = exportService.export(principal, salonId, from, to, type, report);
    return asAttachment(file);
  }

  /** Builds a file-download response. */
  static ResponseEntity<byte[]> asAttachment(ExportedFile file) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.filename()).build().toString())
        .contentType(MediaType.parseMediaType(file.contentType()))
        .body(file.content());
  }
}
