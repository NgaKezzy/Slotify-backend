package com.slotify.module.staff.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An absence of a staff member (holiday, sick leave, …) as a UTC instant range (table {@code
 * staff_time_off}). Approved entries are subtracted from the working windows by the availability
 * engine.
 */
@Entity
@Table(name = "staff_time_off")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffTimeOff extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @Column(length = 255)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TimeOffStatus status = TimeOffStatus.APPROVED;

  /** Creates a time-off entry with the given status. */
  public static StaffTimeOff of(
      Staff staff, Instant startAt, Instant endAt, String reason, TimeOffStatus status) {
    StaffTimeOff timeOff = new StaffTimeOff();
    timeOff.staff = staff;
    timeOff.startAt = startAt;
    timeOff.endAt = endAt;
    timeOff.reason = reason;
    timeOff.status = status;
    return timeOff;
  }
}
