package com.slotify.module.staff.mapper;

import com.slotify.module.salon.entity.Salon;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.staff.dto.PublicStaffResponse;
import com.slotify.module.staff.dto.ShiftOverrideResponse;
import com.slotify.module.staff.dto.ShiftResponse;
import com.slotify.module.staff.dto.StaffMeResponse;
import com.slotify.module.staff.dto.StaffResponse;
import com.slotify.module.staff.dto.TimeOffResponse;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.staff.entity.StaffShiftOverride;
import com.slotify.module.staff.entity.StaffTimeOff;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper between the staff entities and their DTOs. */
@Mapper
public interface StaffMapper {

  @Mapping(target = "salonId", source = "salon.id")
  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "email", source = "user.email")
  @Mapping(target = "serviceIds", source = "services")
  StaffResponse toResponse(Staff staff);

  @Mapping(target = "serviceIds", source = "services")
  PublicStaffResponse toPublicResponse(Staff staff);

  List<PublicStaffResponse> toPublicResponses(List<Staff> staff);

  StaffMeResponse.SalonSummary toSalonSummary(Salon salon);

  @Mapping(target = "dayOfWeek", expression = "java(shift.day())")
  ShiftResponse toResponse(StaffShift shift);

  List<ShiftResponse> toShiftResponses(List<StaffShift> shifts);

  ShiftOverrideResponse toResponse(StaffShiftOverride override);

  List<ShiftOverrideResponse> toOverrideResponses(List<StaffShiftOverride> overrides);

  TimeOffResponse toResponse(StaffTimeOff timeOff);

  List<TimeOffResponse> toTimeOffResponses(List<StaffTimeOff> timeOff);

  /** Flattens the service association to its ids (sorted for stable output). */
  default List<Long> toServiceIds(Collection<SalonService> services) {
    return services.stream().map(SalonService::getId).sorted().toList();
  }
}
