package com.slotify.module.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.report.dto.Granularity;
import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.report.service.SeriesBucketer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Bucketing of instants into day / week / month series in a timezone. */
class SeriesBucketerTest {

  private static final ZoneId SAIGON = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  @Test
  void dayBucketUsesTheLocalDateOfTheZone() {
    // 23:30 UTC on 1 March is already 2 March in UTC+7.
    Instant lateEvening = Instant.parse("2026-03-01T23:30:00Z");
    assertThat(SeriesBucketer.bucketOf(lateEvening, SAIGON, Granularity.DAY))
        .isEqualTo(LocalDate.of(2026, 3, 2));
    assertThat(SeriesBucketer.bucketOf(lateEvening, ZoneId.of("UTC"), Granularity.DAY))
        .isEqualTo(LocalDate.of(2026, 3, 1));
  }

  @Test
  void weekBucketStartsOnMonday() {
    // 2026-03-05 is a Thursday; the ISO week starts on Monday 2026-03-02.
    Instant thursday = Instant.parse("2026-03-05T10:00:00Z");
    assertThat(SeriesBucketer.bucketOf(thursday, BERLIN, Granularity.WEEK))
        .isEqualTo(LocalDate.of(2026, 3, 2));
    assertThat(SeriesBucketer.bucketStart(LocalDate.of(2026, 3, 2), Granularity.WEEK))
        .isEqualTo(LocalDate.of(2026, 3, 2));
  }

  @Test
  void monthBucketStartsOnTheFirst() {
    Instant midMonth = Instant.parse("2026-03-17T10:00:00Z");
    assertThat(SeriesBucketer.bucketOf(midMonth, BERLIN, Granularity.MONTH))
        .isEqualTo(LocalDate.of(2026, 3, 1));
  }

  @Test
  void bucketsCoverTheWholePeriodIncludingPartialFirstBucket() {
    ReportPeriod period =
        new ReportPeriod(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 20), BERLIN);
    assertThat(SeriesBucketer.buckets(period, Granularity.DAY)).hasSize(17);
    assertThat(SeriesBucketer.buckets(period, Granularity.WEEK))
        .containsExactly(
            LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 16));
    assertThat(SeriesBucketer.buckets(period, Granularity.MONTH))
        .containsExactly(LocalDate.of(2026, 3, 1));
  }
}
