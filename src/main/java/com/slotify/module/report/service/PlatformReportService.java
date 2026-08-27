package com.slotify.module.report.service;

import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import com.slotify.module.payment.entity.Refund;
import com.slotify.module.payment.repository.PaymentRepository;
import com.slotify.module.payment.repository.RefundRepository;
import com.slotify.module.payment.repository.SalonAmount;
import com.slotify.module.report.dto.PayoutsReportResponse;
import com.slotify.module.report.dto.PlatformOverviewResponse;
import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.report.export.ExportType;
import com.slotify.module.report.export.ExportedFile;
import com.slotify.module.report.export.MoneyFormat;
import com.slotify.module.report.export.ReportTable;
import com.slotify.module.report.repository.ReportRepository;
import com.slotify.module.report.repository.SalonRevenueRow;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-wide reports for the super admin: overview and payouts (plan §3.6 / §3.7).
 *
 * <p>Platform periods are interpreted in UTC because they span salons in different zones.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformReportService {

  /** Payment states counted as money received. */
  static final List<PaymentState> PAID_STATES =
      List.of(PaymentState.SUCCEEDED, PaymentState.PARTIAL_REFUND);

  /** Providers whose money lands on the platform account (cash stays at the salon). */
  static final List<PaymentProvider> ONLINE_PROVIDERS =
      List.of(PaymentProvider.STRIPE, PaymentProvider.PAYPAL);

  private static final int TOP_SALONS = 5;

  private final ReportRepository reportRepository;
  private final SalonRepository salonRepository;
  private final UserRepository userRepository;
  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final ReportExportService exportService;
  private final Clock clock;

  /** Counts by salon status and user role plus the last 30 days of bookings and revenue. */
  public PlatformOverviewResponse overview() {
    ReportPeriod period = ReportPeriod.resolve(null, null, ZoneOffset.UTC, clock);
    Map<SalonStatus, Long> salons = new EnumMap<>(SalonStatus.class);
    for (SalonStatus status : SalonStatus.values()) {
      salons.put(status, salonRepository.countByStatus(status));
    }
    Map<Role, Long> users = new EnumMap<>(Role.class);
    for (Role role : Role.values()) {
      users.put(role, userRepository.countByRole(role));
    }
    List<PlatformOverviewResponse.CurrencyRevenue> revenue =
        reportRepository
            .revenueByCurrency(BookingStatus.COMPLETED, period.startInstant(), period.endInstant())
            .stream()
            .map(
                r ->
                    new PlatformOverviewResponse.CurrencyRevenue(
                        r.currency(), r.revenueMinor(), r.bookings()))
            .toList();
    List<PlatformOverviewResponse.SalonRevenue> top =
        reportRepository
            .topSalonsByRevenue(
                BookingStatus.COMPLETED,
                period.startInstant(),
                period.endInstant(),
                Limit.of(TOP_SALONS))
            .stream()
            .map(
                r ->
                    new PlatformOverviewResponse.SalonRevenue(
                        r.salonId(), r.salonName(), r.currency(), r.revenueMinor(), r.bookings()))
            .toList();
    return new PlatformOverviewResponse(
        period.toDto(),
        salons,
        users,
        reportRepository.countByStartAtGreaterThanEqualAndStartAtLessThan(
            period.startInstant(), period.endInstant()),
        revenue,
        top);
  }

  /** Online revenue, commission and payout per ACTIVE salon for payments made in the period. */
  public PayoutsReportResponse payouts(LocalDate from, LocalDate to) {
    ReportPeriod period = ReportPeriod.resolve(from, to, ZoneOffset.UTC, clock);
    Map<Long, Long> paid =
        toMap(
            paymentRepository.sumPaidBySalon(
                PAID_STATES, ONLINE_PROVIDERS, period.startInstant(), period.endInstant()));
    Map<Long, Long> refunded =
        toMap(
            refundRepository.sumRefundedBySalon(
                Refund.Status.SUCCEEDED,
                PAID_STATES,
                ONLINE_PROVIDERS,
                period.startInstant(),
                period.endInstant()));
    Map<Long, Long> cash =
        reportRepository
            .revenueBySalonAndMethod(
                BookingStatus.COMPLETED,
                PaymentMethod.CASH,
                period.startInstant(),
                period.endInstant())
            .stream()
            .collect(Collectors.toMap(SalonRevenueRow::salonId, SalonRevenueRow::revenueMinor));

    List<PayoutsReportResponse.Row> rows = new ArrayList<>();
    for (Salon salon : salonRepository.findAllByStatusOrderByNameAsc(SalonStatus.ACTIVE)) {
      long online = paid.getOrDefault(salon.getId(), 0L) - refunded.getOrDefault(salon.getId(), 0L);
      long commission = PayoutMath.commission(online, salon.getCommissionPercent());
      rows.add(
          new PayoutsReportResponse.Row(
              salon.getId(),
              salon.getName(),
              salon.getCurrency(),
              salon.getCommissionPercent(),
              online,
              commission,
              online - commission,
              cash.getOrDefault(salon.getId(), 0L)));
    }
    return new PayoutsReportResponse(period.toDto(), rows);
  }

  /** The payouts report as a downloadable file. */
  public ExportedFile exportPayouts(LocalDate from, LocalDate to, ExportType type) {
    PayoutsReportResponse report = payouts(from, to);
    List<List<String>> rows =
        report.rows().stream()
            .map(
                r ->
                    List.of(
                        String.valueOf(r.salonId()),
                        r.salonName(),
                        r.currency(),
                        r.commissionPercent().toPlainString() + " %",
                        MoneyFormat.format(r.onlineRevenueMinor(), r.currency()),
                        MoneyFormat.format(r.commissionMinor(), r.currency()),
                        MoneyFormat.format(r.payoutMinor(), r.currency()),
                        MoneyFormat.format(r.cashRevenueMinor(), r.currency())))
            .toList();
    ReportTable table =
        new ReportTable(
            "Payouts report",
            exportService.subtitles(report.period()),
            List.of(
                "Salon id",
                "Salon",
                "Currency",
                "Commission",
                "Online revenue",
                "Commission amount",
                "Payout",
                "Cash revenue (info)"),
            rows);
    return exportService.render(table, type, "payouts-report");
  }

  private static Map<Long, Long> toMap(List<SalonAmount> amounts) {
    return amounts.stream()
        .collect(Collectors.toMap(SalonAmount::salonId, SalonAmount::amountMinor, Long::sum));
  }
}
