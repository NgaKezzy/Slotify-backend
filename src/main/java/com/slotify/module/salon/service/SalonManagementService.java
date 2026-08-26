package com.slotify.module.salon.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.salon.dto.OpeningHourDto;
import com.slotify.module.salon.dto.SalonDetailResponse;
import com.slotify.module.salon.dto.SalonRequest;
import com.slotify.module.salon.dto.SalonSettingsDto;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.entity.Amenity;
import com.slotify.module.salon.entity.Category;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonOpeningHour;
import com.slotify.module.salon.entity.SalonSettings;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.mapper.SalonMapper;
import com.slotify.module.salon.repository.AmenityRepository;
import com.slotify.module.salon.repository.CategoryRepository;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import com.slotify.security.UserPrincipal;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salon administration by owners ({@code /admin/salons}) and the platform ({@code
 * /admin/platform/salons}): create, update, opening hours, settings, images, approval.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SalonManagementService {

  private final SalonRepository salonRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final AmenityRepository amenityRepository;
  private final SalonAccess salonAccess;
  private final SlugGenerator slugGenerator;
  private final SalonMapper mapper;

  // ---- Owner --------------------------------------------------------------------------------

  /**
   * Creates a salon in PENDING state. A CUSTOMER creating their first salon is promoted to
   * SALON_OWNER; approval by a SUPER_ADMIN makes it visible.
   */
  public SalonDetailResponse create(UserPrincipal principal, SalonRequest request) {
    User owner =
        userRepository
            .findById(principal.id())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    if (owner.getRole() == Role.CUSTOMER) {
      owner.setRole(Role.SALON_OWNER);
    } else if (owner.getRole() == Role.STAFF) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    validateTimezone(request.timezone());

    Salon salon =
        Salon.create(owner, request.name().trim(), slugGenerator.uniqueSlugFor(request.name()));
    applyRequest(salon, request);
    salon.replaceOpeningHours(defaultOpeningHours());
    return mapper.toDetail(salonRepository.save(salon));
  }

  @Transactional(readOnly = true)
  public List<SalonSummaryResponse> listOwned(UserPrincipal principal) {
    List<Salon> salons =
        principal.hasRole(Role.SUPER_ADMIN)
            ? salonRepository.findAll()
            : salonRepository.findAllByOwnerIdOrderByNameAsc(principal.id());
    return salons.stream().map(mapper::toSummary).toList();
  }

  @Transactional(readOnly = true)
  public SalonDetailResponse getOwned(Long salonId, UserPrincipal principal) {
    return mapper.toDetail(salonAccess.requireOwned(salonId, principal));
  }

  public SalonDetailResponse update(Long salonId, UserPrincipal principal, SalonRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    validateTimezone(request.timezone());
    salon.setName(request.name().trim());
    applyRequest(salon, request);
    return mapper.toDetail(salon);
  }

  public List<OpeningHourDto> updateOpeningHours(
      Long salonId, UserPrincipal principal, List<OpeningHourDto> hours) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    Map<DayOfWeek, OpeningHourDto> byDay = new EnumMap<>(DayOfWeek.class);
    for (OpeningHourDto dto : hours) {
      if (!dto.closed()
          && (dto.openTime() == null
              || dto.closeTime() == null
              || !dto.openTime().isBefore(dto.closeTime()))) {
        throw new AppException(ErrorCode.VALIDATION_FAILED);
      }
      byDay.put(dto.day(), dto);
    }
    List<SalonOpeningHour> entities =
        java.util.Arrays.stream(DayOfWeek.values())
            .map(
                day -> {
                  OpeningHourDto dto = byDay.get(day);
                  return dto == null
                      ? SalonOpeningHour.of(day, null, null, true)
                      : SalonOpeningHour.of(day, dto.openTime(), dto.closeTime(), dto.closed());
                })
            .toList();
    salon.replaceOpeningHours(entities);
    return salon.getOpeningHours().stream().map(mapper::toDto).toList();
  }

  public SalonSettingsDto updateSettings(
      Long salonId, UserPrincipal principal, SalonSettingsDto dto) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    SalonSettings settings = salon.getSettings();
    settings.setSlotIntervalMin(dto.slotIntervalMin());
    settings.setMinAdvanceBookingMin(dto.minAdvanceBookingMin());
    settings.setMaxAdvanceDays(dto.maxAdvanceDays());
    settings.setCancelBeforeMin(dto.cancelBeforeMin());
    settings.setAutoConfirm(dto.autoConfirm());
    settings.setRequireDeposit(dto.requireDeposit());
    settings.setDepositPercent(dto.depositPercent());
    settings.setAcceptStripe(dto.acceptStripe());
    settings.setAcceptPaypal(dto.acceptPaypal());
    settings.setAcceptCash(dto.acceptCash());
    return mapper.toDto(settings);
  }

  public List<String> updateImages(Long salonId, UserPrincipal principal, List<String> urls) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    salon.replaceImages(urls);
    return mapper.imageUrls(salon);
  }

  // ---- Platform (SUPER_ADMIN) ----------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<SalonSummaryResponse> listAll(SalonStatus status) {
    List<Salon> salons =
        status == null
            ? salonRepository.findAll()
            : salonRepository.findAll(
                com.slotify.module.salon.repository.SalonSpecifications.hasStatus(status));
    return salons.stream().map(mapper::toSummary).toList();
  }

  public SalonSummaryResponse changeStatus(Long salonId, SalonStatus status) {
    Salon salon =
        salonRepository
            .findById(salonId)
            .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    salon.setStatus(status);
    return mapper.toSummary(salon);
  }

  public SalonSummaryResponse updateCommission(Long salonId, java.math.BigDecimal percent) {
    Salon salon =
        salonRepository
            .findById(salonId)
            .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    salon.setCommissionPercent(percent);
    return mapper.toSummary(salon);
  }

  // ---- helpers --------------------------------------------------------------------------------

  private void applyRequest(Salon salon, SalonRequest request) {
    salon.setDescription(request.description());
    salon.setPhone(request.phone());
    salon.setEmail(request.email());
    salon.setAddress(request.address().trim());
    salon.setCity(request.city().trim());
    salon.setCountry(request.country());
    salon.setLat(request.lat());
    salon.setLng(request.lng());
    salon.setTimezone(request.timezone());
    salon.setCurrency(request.currency());
    salon.setCoverUrl(request.coverUrl());
    if (request.categoryIds() != null) {
      List<Category> categories = categoryRepository.findAllById(request.categoryIds());
      salon.setCategories(new LinkedHashSet<>(categories));
    }
    if (request.amenityIds() != null) {
      List<Amenity> amenities = amenityRepository.findAllById(request.amenityIds());
      salon.setAmenities(new LinkedHashSet<>(amenities));
    }
  }

  private static void validateTimezone(String timezone) {
    try {
      ZoneId.of(timezone);
    } catch (RuntimeException ex) {
      throw new AppException(ErrorCode.VALIDATION_FAILED);
    }
  }

  /** Mon–Sat 09:00–18:00, closed on Sunday. */
  private static List<SalonOpeningHour> defaultOpeningHours() {
    return java.util.Arrays.stream(DayOfWeek.values())
        .map(
            day ->
                day == DayOfWeek.SUNDAY
                    ? SalonOpeningHour.of(day, null, null, true)
                    : SalonOpeningHour.of(
                        day, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0), false))
        .toList();
  }
}
