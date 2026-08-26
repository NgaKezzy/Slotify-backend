package com.slotify.module.salon.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Opening window of a salon for one weekday, in the salon's local time.
 *
 * <p>{@code dayOfWeek} is stored as 0 = Monday … 6 = Sunday (i.e. {@link DayOfWeek#getValue()} -
 * 1).
 */
@Entity
@Table(name = "salon_opening_hours")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalonOpeningHour extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "day_of_week", nullable = false)
  private int dayOfWeek;

  @Column(name = "open_time")
  private LocalTime openTime;

  @Column(name = "close_time")
  private LocalTime closeTime;

  @Column(name = "is_closed", nullable = false)
  private boolean closed;

  public static SalonOpeningHour of(
      DayOfWeek day, LocalTime openTime, LocalTime closeTime, boolean closed) {
    SalonOpeningHour hour = new SalonOpeningHour();
    hour.dayOfWeek = toStoredDay(day);
    hour.openTime = openTime;
    hour.closeTime = closeTime;
    hour.closed = closed;
    return hour;
  }

  public DayOfWeek day() {
    return DayOfWeek.of(dayOfWeek + 1);
  }

  /** Converts a {@link DayOfWeek} to the 0-based storage value. */
  public static int toStoredDay(DayOfWeek day) {
    return day.getValue() - 1;
  }
}
