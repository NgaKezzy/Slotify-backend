package com.slotify.module.report.service;

import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.report.dto.BookingsReportResponse;
import com.slotify.module.report.dto.Granularity;
import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.report.dto.RevenueReportResponse;
import com.slotify.module.report.dto.StaffPerformanceResponse;
import com.slotify.module.report.export.ExcelReportWriter;
import com.slotify.module.report.export.ExportType;
import com.slotify.module.report.export.ExportedFile;
import com.slotify.module.report.export.MoneyFormat;
import com.slotify.module.report.export.PdfReportWriter;
import com.slotify.module.report.export.ReportKind;
import com.slotify.module.report.export.ReportTable;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Turns the salon reports into downloadable Excel / PDF files. */
@Service
@RequiredArgsConstructor
public class ReportExportService {

  private static final DateTimeFormatter GENERATED_AT =
      DateTimeFormatter.ISO_INSTANT.withZone(java.time.ZoneOffset.UTC);

  private final ReportService reportService;
  private final ExcelReportWriter excelWriter;
  private final PdfReportWriter pdfWriter;
  private final Clock clock;

  /** Builds the requested report for the period (daily series) and renders it. */
  public ExportedFile export(
      UserPrincipal principal,
      Long salonId,
      LocalDate from,
      LocalDate to,
      ExportType type,
      ReportKind kind) {
    ReportTable table =
        switch (kind) {
          case REVENUE ->
              revenueTable(reportService.revenue(principal, salonId, from, to, Granularity.DAY));
          case BOOKINGS ->
              bookingsTable(reportService.bookings(principal, salonId, from, to, Granularity.DAY));
          case STAFF -> staffTable(reportService.staffPerformance(principal, salonId, from, to));
        };
    return render(table, type, kind.name().toLowerCase() + "-report");
  }

  /** Renders a table with the given format and base file name. */
  public ExportedFile render(ReportTable table, ExportType type, String baseName) {
    byte[] bytes =
        switch (type) {
          case EXCEL -> excelWriter.write(table);
          case PDF -> pdfWriter.write(table);
        };
    return new ExportedFile(baseName + "." + type.extension(), type.contentType(), bytes);
  }

  /** Subtitle lines shared by every export: period and generation time. */
  public List<String> subtitles(ReportPeriod.PeriodDto period) {
    return List.of(
        "Period: " + period.from() + " to " + period.to() + " (" + period.timezone() + ")",
        "Generated at: " + GENERATED_AT.format(Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)));
  }

  private ReportTable revenueTable(RevenueReportResponse report) {
    List<List<String>> rows = new ArrayList<>();
    for (RevenueReportResponse.Point point : report.series()) {
      rows.add(
          List.of(
              point.periodStart().toString(),
              String.valueOf(point.bookings()),
              MoneyFormat.format(point.revenueMinor(), report.currency())));
    }
    rows.add(
        List.of(
            "Total",
            String.valueOf(report.series().stream().mapToLong(p -> p.bookings()).sum()),
            MoneyFormat.format(report.totalRevenueMinor(), report.currency())));
    return new ReportTable(
        "Revenue report",
        subtitles(report.period()),
        List.of("Date", "Completed bookings", "Revenue"),
        rows);
  }

  private ReportTable bookingsTable(BookingsReportResponse report) {
    List<String> headers = new ArrayList<>(List.of("Date", "Total"));
    Arrays.stream(BookingStatus.values()).map(BookingStatus::name).forEach(headers::add);
    List<List<String>> rows = new ArrayList<>();
    for (BookingsReportResponse.Point point : report.series()) {
      List<String> row = new ArrayList<>();
      row.add(point.periodStart().toString());
      row.add(String.valueOf(point.total()));
      for (BookingStatus status : BookingStatus.values()) {
        row.add(String.valueOf(point.counts().getOrDefault(status, 0L)));
      }
      rows.add(row);
    }
    List<String> totals = new ArrayList<>();
    totals.add("Total");
    totals.add(String.valueOf(report.totals().values().stream().mapToLong(Long::longValue).sum()));
    for (BookingStatus status : BookingStatus.values()) {
      totals.add(String.valueOf(report.totals().getOrDefault(status, 0L)));
    }
    rows.add(totals);
    return new ReportTable("Bookings report", subtitles(report.period()), headers, rows);
  }

  private ReportTable staffTable(StaffPerformanceResponse report) {
    List<List<String>> rows = new ArrayList<>();
    for (StaffPerformanceResponse.Row row : report.staff()) {
      rows.add(
          List.of(
              row.displayName(),
              row.active() ? "Yes" : "No",
              String.valueOf(row.bookings()),
              String.valueOf(row.completed()),
              String.valueOf(row.noShows()),
              MoneyFormat.format(row.revenueMinor(), report.currency()),
              row.ratingAvg().toPlainString(),
              row.utilisationPercent() + " %"));
    }
    return new ReportTable(
        "Staff performance report",
        subtitles(report.period()),
        List.of(
            "Staff",
            "Active",
            "Bookings",
            "Completed",
            "No-shows",
            "Revenue",
            "Rating",
            "Utilisation"),
        rows);
  }
}
