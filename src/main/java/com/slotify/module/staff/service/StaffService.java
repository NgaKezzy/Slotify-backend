package com.slotify.module.staff.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.staff.dto.CreateStaffRequest;
import com.slotify.module.staff.dto.PublicStaffResponse;
import com.slotify.module.staff.dto.ReplaceShiftsRequest;
import com.slotify.module.staff.dto.ShiftOverrideRequest;
import com.slotify.module.staff.dto.ShiftOverrideResponse;
import com.slotify.module.staff.dto.ShiftResponse;
import com.slotify.module.staff.dto.StaffResponse;
import com.slotify.module.staff.dto.StaffServicesRequest;
import com.slotify.module.staff.dto.TimeOffRequest;
import com.slotify.module.staff.dto.TimeOffResponse;
import com.slotify.module.staff.dto.UpdateStaffRequest;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.staff.entity.StaffShiftOverride;
import com.slotify.module.staff.entity.StaffTimeOff;
import com.slotify.module.staff.entity.TimeOffStatus;
import com.slotify.module.staff.mapper.StaffMapper;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftOverrideRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.module.staff.repository.StaffTimeOffRepository;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salon-owner management of staff members: profile CRUD, the services they perform, the weekly
 * shift schedule, per-date overrides and time off. Also serves the public staff listing.
 *
 * <p>Every owner method first runs {@link SalonAccess#requireOwned} so that a {@code SALON_OWNER}
 * can only touch their own salons. The entity-level {@link #create(Salon, CreateStaffRequest)} is
 * public so that the demo seeder can reuse it.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

  private final StaffRepository staffRepository;
  private final StaffShiftRepository shiftRepository;
  private final StaffShiftOverrideRepository overrideRepository;
  private final StaffTimeOffRepository timeOffRepository;
  private final SalonServiceRepository salonServiceRepository;
  private final SalonRepository salonRepository;
  private final SalonAccess salonAccess;
  private final StaffInviteService inviteService;
  private final StaffMapper mapper;
  private final Clock clock;

  // ---- public catalog --------------------------------------------------------------------------

  /**
   * Active staff of an ACTIVE salon with the ids of the services they perform.
   *
   * @throws AppException {@link ErrorCode#SALON_NOT_FOUND} when the salon is not publicly visible
   */
  @Transactional(readOnly = true)
  public List<PublicStaffResponse> listPublic(Long salonId) {
    salonRepository
        .findByIdAndStatus(salonId, SalonStatus.ACTIVE)
        .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    return mapper.toPublicResponses(
        staffRepository.findAllBySalonIdAndActiveTrueOrderByDisplayNameAsc(salonId));
  }

  // ---- staff profile ---------------------------------------------------------------------------

  /** Creates a staff member for the owner's salon, inviting them by email when requested. */
  public StaffResponse create(Long salonId, UserPrincipal principal, CreateStaffRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    return mapper.toResponse(create(salon, request));
  }

  /**
   * Creates a staff member of {@code salon}. When {@code inviteEmail} is present, a {@code STAFF}
   * login account is created or linked and an invitation email is sent.
   */
  public Staff create(Salon salon, CreateStaffRequest request) {
    Staff staff = Staff.create(salon, request.displayName().trim());
    staff.setTitle(request.title());
    staff.setBio(request.bio());
    staff.setAvatarUrl(request.avatarUrl());
    if (request.inviteEmail() != null && !request.inviteEmail().isBlank()) {
      staff.setUser(inviteService.invite(salon, request.inviteEmail(), staff.getDisplayName()));
    }
    return staffRepository.save(staff);
  }

  /** All staff (active and inactive) of the salon. */
  @Transactional(readOnly = true)
  public List<StaffResponse> list(Long salonId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    return staffRepository.findAllBySalonIdOrderByDisplayNameAsc(salonId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public StaffResponse get(Long salonId, Long staffId, UserPrincipal principal) {
    return mapper.toResponse(requireStaff(salonId, staffId, principal));
  }

  /** Updates the profile fields and the bookable flag. */
  public StaffResponse update(
      Long salonId, Long staffId, UserPrincipal principal, UpdateStaffRequest request) {
    Staff staff = requireStaff(salonId, staffId, principal);
    staff.setDisplayName(request.displayName().trim());
    staff.setTitle(request.title());
    staff.setBio(request.bio());
    staff.setAvatarUrl(request.avatarUrl());
    staff.setActive(request.active());
    return mapper.toResponse(staff);
  }

  /** Soft-deletes the staff member; past bookings keep referencing the row. */
  public void delete(Long salonId, Long staffId, UserPrincipal principal) {
    Staff staff = requireStaff(salonId, staffId, principal);
    staff.setActive(false);
    staff.setDeletedAt(Instant.now(clock));
  }

  // ---- services --------------------------------------------------------------------------------

  /**
   * Replaces the set of services the staff member can perform.
   *
   * @throws AppException {@link ErrorCode#STAFF_CANNOT_PERFORM_SERVICE} when an id does not belong
   *     to an active service of this salon
   */
  public StaffResponse replaceServices(
      Long salonId, Long staffId, UserPrincipal principal, StaffServicesRequest request) {
    Staff staff = requireStaff(salonId, staffId, principal);
    Set<Long> ids = request.serviceIds();
    List<SalonService> services =
        salonServiceRepository.findAllByIdInAndSalonIdAndActiveTrue(ids, salonId);
    if (services.size() != ids.size()) {
      throw new AppException(ErrorCode.STAFF_CANNOT_PERFORM_SERVICE);
    }
    staff.replaceServices(services);
    return mapper.toResponse(staff);
  }

  // ---- weekly shifts ---------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ShiftResponse> getShifts(Long salonId, Long staffId, UserPrincipal principal) {
    Staff staff = requireStaff(salonId, staffId, principal);
    return mapper.toShiftResponses(
        shiftRepository.findAllByStaffIdOrderByDayOfWeekAscStartTimeAsc(staff.getId()));
  }

  /**
   * Replaces the whole weekly schedule.
   *
   * @throws AppException {@link ErrorCode#INVALID_TIME_RANGE} or {@link ErrorCode#SHIFT_OVERLAP}
   *     when the windows are inconsistent (see {@link ShiftScheduleValidator})
   */
  public List<ShiftResponse> replaceShifts(
      Long salonId, Long staffId, UserPrincipal principal, ReplaceShiftsRequest request) {
    Staff staff = requireStaff(salonId, staffId, principal);
    ShiftScheduleValidator.validate(request.shifts());
    shiftRepository.deleteAllByStaffId(staff.getId());
    List<StaffShift> shifts =
        request.shifts().stream()
            .map(s -> StaffShift.of(staff, s.dayOfWeek(), s.startTime(), s.endTime()))
            .toList();
    return mapper.toShiftResponses(shiftRepository.saveAll(shifts));
  }

  // ---- per-date overrides ----------------------------------------------------------------------

  /** Overrides from the salon-local today onwards, soonest first. */
  @Transactional(readOnly = true)
  public List<ShiftOverrideResponse> listOverrides(
      Long salonId, Long staffId, UserPrincipal principal) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    Staff staff = requireStaff(salonId, staffId, principal);
    LocalDate today = LocalDate.now(clock.withZone(salon.zoneId()));
    return overrideRepository
        .findAllByStaffIdAndDateGreaterThanEqualOrderByDateAsc(staff.getId(), today)
        .stream()
        .map(o -> mapper.toResponse(o))
        .toList();
  }

  /** Creates the exception for a date, replacing an existing one for the same date. */
  public ShiftOverrideResponse upsertOverride(
      Long salonId, Long staffId, UserPrincipal principal, ShiftOverrideRequest request) {
    Staff staff = requireStaff(salonId, staffId, principal);
    if (!request.off()) {
      ShiftScheduleValidator.requireOrdered(request.startTime(), request.endTime());
    }
    overrideRepository
        .findByStaffIdAndDate(staff.getId(), request.date())
        .ifPresent(overrideRepository::delete);
    StaffShiftOverride override =
        request.off()
            ? StaffShiftOverride.dayOff(staff, request.date())
            : StaffShiftOverride.window(
                staff, request.date(), request.startTime(), request.endTime());
    return mapper.toResponse(overrideRepository.save(override));
  }

  public void deleteOverride(Long salonId, Long staffId, Long overrideId, UserPrincipal principal) {
    Staff staff = requireStaff(salonId, staffId, principal);
    StaffShiftOverride override =
        overrideRepository
            .findByIdAndStaffId(overrideId, staff.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    overrideRepository.delete(override);
  }

  // ---- time off --------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<TimeOffResponse> listTimeOff(Long salonId, Long staffId, UserPrincipal principal) {
    Staff staff = requireStaff(salonId, staffId, principal);
    return mapper.toTimeOffResponses(
        timeOffRepository.findAllByStaffIdOrderByStartAtDesc(staff.getId()));
  }

  /** Creates an already-approved absence on behalf of the staff member. */
  public TimeOffResponse createTimeOff(
      Long salonId, Long staffId, UserPrincipal principal, TimeOffRequest request) {
    Staff staff = requireStaff(salonId, staffId, principal);
    return mapper.toResponse(createTimeOff(staff, request, TimeOffStatus.APPROVED));
  }

  /**
   * Stores an absence for {@code staff} with the given status.
   *
   * @throws AppException {@link ErrorCode#INVALID_TIME_RANGE} when {@code endAt} is not after
   *     {@code startAt}
   */
  public StaffTimeOff createTimeOff(Staff staff, TimeOffRequest request, TimeOffStatus status) {
    if (!request.endAt().isAfter(request.startAt())) {
      throw new AppException(ErrorCode.INVALID_TIME_RANGE);
    }
    return timeOffRepository.save(
        StaffTimeOff.of(staff, request.startAt(), request.endAt(), request.reason(), status));
  }

  public void deleteTimeOff(Long salonId, Long staffId, Long timeOffId, UserPrincipal principal) {
    Staff staff = requireStaff(salonId, staffId, principal);
    deleteTimeOff(staff, timeOffId);
  }

  /** Deletes an absence of {@code staff}; {@link ErrorCode#NOT_FOUND} when it is not theirs. */
  public void deleteTimeOff(Staff staff, Long timeOffId) {
    StaffTimeOff timeOff =
        timeOffRepository
            .findByIdAndStaffId(timeOffId, staff.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    timeOffRepository.delete(timeOff);
  }

  // ---------------------------------------------------------------------------------------------

  private Staff requireStaff(Long salonId, Long staffId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    return staffRepository
        .findByIdAndSalonId(staffId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
  }
}
