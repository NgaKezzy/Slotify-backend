package com.slotify.module.staff.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A one-day exception to the weekly {@link StaffShift} schedule (table {@code
 * staff_shift_overrides}): either a whole day off ({@code off = true}) or a replacement working
 * window for that date. At most one override exists per staff member and date.
 */
@Entity
@Table(name = "staff_shift_overrides")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffShiftOverride extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  /** Local date in the salon timezone. */
  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  /** {@code true} = the staff member does not work at all on this date; times are ignored. */
  @Column(name = "is_off", nullable = false)
  private boolean off;

  /** Creates a whole-day-off override. */
  public static StaffShiftOverride dayOff(Staff staff, LocalDate date) {
    StaffShiftOverride override = new StaffShiftOverride();
    override.staff = staff;
    override.date = date;
    override.off = true;
    return override;
  }

  /** Creates an override that replaces the weekly shifts with a single window on that date. */
  public static StaffShiftOverride window(
      Staff staff, LocalDate date, LocalTime startTime, LocalTime endTime) {
    StaffShiftOverride override = new StaffShiftOverride();
    override.staff = staff;
    override.date = date;
    override.startTime = startTime;
    override.endTime = endTime;
    return override;
  }
}
