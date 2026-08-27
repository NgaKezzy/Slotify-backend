package com.slotify.module.salon.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.salon.dto.SalonDetailResponse;
import com.slotify.module.salon.dto.SalonSearchRequest;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.mapper.SalonMapper;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.salon.repository.SalonSpecifications;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public, read-only salon queries (search and detail) restricted to ACTIVE salons. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalonQueryService {

  private final SalonRepository salonRepository;
  private final SalonMapper mapper;

  /**
   * Searches active salons. When a location is given, results carry a distance and can be sorted
   * by it; they are only restricted to a radius when {@code radiusKm} is set explicitly (a search
   * with "any distance" from a far-away device still returns the catalogue).
   */
  public PageResponse<SalonSummaryResponse> search(SalonSearchRequest request) {
    SalonSearchRequest.Sort sort = SalonSearchRequest.Sort.parse(request.sort());
    Double radius = request.radiusKm();
    boolean withinRadius = request.hasLocation() && radius != null;

    // Optional filters return null when not requested; Spring Data rejects null specs.
    List<Specification<Salon>> filters =
        java.util.stream.Stream.of(
                SalonSpecifications.hasStatus(SalonStatus.ACTIVE),
                SalonSpecifications.matchesText(request.q()),
                SalonSpecifications.inCity(request.city()),
                SalonSpecifications.hasCategory(request.categoryId()),
                withinRadius
                    ? SalonSpecifications.withinBoundingBox(request.lat(), request.lng(), radius)
                    : null)
            .filter(java.util.Objects::nonNull)
            .toList();
    Specification<Salon> spec = Specification.allOf(filters);

    Sort dbSort =
        switch (sort) {
          case NAME -> Sort.by("name").ascending();
          case RATING, DISTANCE ->
              Sort.by("ratingAvg").descending().and(Sort.by("ratingCount").descending());
        };
    Page<Salon> page =
        salonRepository.findAll(
            spec, PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(), dbSort));

    List<SalonSummaryResponse> items =
        page.getContent().stream().map(salon -> toSummaryWithDistance(salon, request)).toList();
    if (request.hasLocation()) {
      items =
          items.stream()
              .filter(s -> !withinRadius || s.distanceKm() == null || s.distanceKm() <= radius)
              .sorted(
                  sort == SalonSearchRequest.Sort.DISTANCE
                      ? Comparator.comparing(
                          SalonSummaryResponse::distanceKm,
                          Comparator.nullsLast(Comparator.naturalOrder()))
                      : Comparator.comparing(SalonSummaryResponse::id, (a, b) -> 0))
              .toList();
    }
    return new PageResponse<>(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  /** Loads an active salon by numeric id or slug. */
  public SalonDetailResponse getPublic(String idOrSlug) {
    return mapper.toDetail(requireActive(idOrSlug));
  }

  /** Resolves an ACTIVE salon by id or slug or throws {@link ErrorCode#SALON_NOT_FOUND}. */
  public Salon requireActive(String idOrSlug) {
    Optional<Salon> salon =
        idOrSlug.chars().allMatch(Character::isDigit)
            ? salonRepository.findByIdAndStatus(Long.parseLong(idOrSlug), SalonStatus.ACTIVE)
            : salonRepository.findBySlug(idOrSlug).filter(Salon::isActive);
    return salon.orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
  }

  private SalonSummaryResponse toSummaryWithDistance(Salon salon, SalonSearchRequest request) {
    SalonSummaryResponse summary = mapper.toSummary(salon);
    if (!request.hasLocation()) {
      return summary;
    }
    return summary.withDistance(
        GeoUtils.distanceKm(request.lat(), request.lng(), salon.getLat(), salon.getLng()));
  }
}
