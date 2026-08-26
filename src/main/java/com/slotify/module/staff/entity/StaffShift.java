package com.slotify.module.staff.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.salon.entity.SalonOpeningHour;
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
 * A recurring weekly working window of a staff member (table {@code staff_shifts}), in the salon's
 * local time. A staff member may have several windows per weekday (e.g. a split shift).
 *
 * <p>{@code dayOfWeek} is stored as 0 = Monday … 6 = Sunday, the same convention as {@link
 * SalonOpeningHour}. The availability engine (Phase 1.4) reads these rows to build working windows.
 */
@Entity
@Table(name = "staff_shifts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffShift extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "day_of_week", nullable = false)
  private int dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  /** Creates a shift window for the given weekday. */
  public static StaffShift of(Staff staff, DayOfWeek day, LocalTime startTime, LocalTime endTime) {
    StaffShift shift = new StaffShift();
    shift.staff = staff;
    shift.dayOfWeek = SalonOpeningHour.toStoredDay(day);
    shift.startTime = startTime;
    shift.endTime = endTime;
    return shift;
  }

  /** The weekday of this shift. */
  public DayOfWeek day() {
    return DayOfWeek.of(dayOfWeek + 1);
  }
}
