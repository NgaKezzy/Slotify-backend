package com.slotify.module.staff;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.staff.dto.ShiftRequest;
import com.slotify.module.staff.service.ShiftScheduleValidator;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the ordering / overlap rules of {@link ShiftScheduleValidator}. */
class ShiftScheduleValidatorTest {

  private static final LocalTime NINE = LocalTime.of(9, 0);
  private static final LocalTime NOON = LocalTime.of(12, 0);
  private static final LocalTime ONE_PM = LocalTime.of(13, 0);
  private static final LocalTime FIVE_PM = LocalTime.of(17, 0);

  @Test
  void acceptsSplitShiftAndTouchingWindows() {
    List<ShiftRequest> shifts =
        List.of(
            new ShiftRequest(DayOfWeek.MONDAY, NINE, NOON),
            new ShiftRequest(DayOfWeek.MONDAY, NOON, FIVE_PM),
            new ShiftRequest(DayOfWeek.TUESDAY, ONE_PM, FIVE_PM));
    assertThatCode(() -> ShiftScheduleValidator.validate(shifts)).doesNotThrowAnyException();
  }

  @Test
  void acceptsEmptySchedule() {
    assertThatCode(() -> ShiftScheduleValidator.validate(List.of())).doesNotThrowAnyException();
  }

  @Test
  void rejectsWindowThatEndsBeforeItStarts() {
    List<ShiftRequest> shifts = List.of(new ShiftRequest(DayOfWeek.MONDAY, NOON, NINE));
    assertThatThrownBy(() -> ShiftScheduleValidator.validate(shifts))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_TIME_RANGE);
  }

  @Test
  void rejectsEmptyWindow() {
    List<ShiftRequest> shifts = List.of(new ShiftRequest(DayOfWeek.MONDAY, NINE, NINE));
    assertThatThrownBy(() -> ShiftScheduleValidator.validate(shifts))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_TIME_RANGE);
  }

  @Test
  void rejectsOverlappingWindowsOnTheSameDay() {
    List<ShiftRequest> shifts =
        List.of(
            new ShiftRequest(DayOfWeek.FRIDAY, ONE_PM, FIVE_PM),
            new ShiftRequest(DayOfWeek.FRIDAY, NINE, LocalTime.of(14, 0)));
    assertThatThrownBy(() -> ShiftScheduleValidator.validate(shifts))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.SHIFT_OVERLAP);
  }

  @Test
  void allowsIdenticalWindowsOnDifferentDays() {
    List<ShiftRequest> shifts =
        List.of(
            new ShiftRequest(DayOfWeek.MONDAY, NINE, FIVE_PM),
            new ShiftRequest(DayOfWeek.SUNDAY, NINE, FIVE_PM));
    assertThatCode(() -> ShiftScheduleValidator.validate(shifts)).doesNotThrowAnyException();
  }
}
