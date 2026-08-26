package com.slotify.module.service.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.module.salon.service.SalonQueryService;
import com.slotify.module.service.dto.ServiceCategoryRequest;
import com.slotify.module.service.dto.ServiceCategoryResponse;
import com.slotify.module.service.dto.ServiceRequest;
import com.slotify.module.service.dto.ServiceResponse;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.entity.ServiceCategory;
import com.slotify.module.service.mapper.ServiceMapper;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.service.repository.ServiceCategoryRepository;
import com.slotify.security.UserPrincipal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service menu of a salon: categories and services (owner CRUD + public listing). */
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceCatalogService {

  private final SalonServiceRepository serviceRepository;
  private final ServiceCategoryRepository categoryRepository;
  private final SalonAccess salonAccess;
  private final SalonQueryService salonQueryService;
  private final ServiceMapper mapper;

  // ---- Public ---------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ServiceResponse> listPublic(String salonIdOrSlug) {
    Salon salon = salonQueryService.requireActive(salonIdOrSlug);
    return mapper.toDtos(
        serviceRepository.findAllBySalonIdAndActiveTrueOrderBySortOrderAscNameAsc(salon.getId()));
  }

  @Transactional(readOnly = true)
  public List<ServiceCategoryResponse> listPublicCategories(String salonIdOrSlug) {
    Salon salon = salonQueryService.requireActive(salonIdOrSlug);
    return mapper.toCategoryDtos(
        categoryRepository.findAllBySalonIdOrderBySortOrderAscNameAsc(salon.getId()));
  }

  // ---- Owner: categories ----------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ServiceCategoryResponse> listCategories(Long salonId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    return mapper.toCategoryDtos(
        categoryRepository.findAllBySalonIdOrderBySortOrderAscNameAsc(salonId));
  }

  public ServiceCategoryResponse createCategory(
      Long salonId, UserPrincipal principal, ServiceCategoryRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    return mapper.toDto(
        categoryRepository.save(
            ServiceCategory.of(salon, request.name().trim(), request.sortOrder())));
  }

  public ServiceCategoryResponse updateCategory(
      Long salonId, Long categoryId, UserPrincipal principal, ServiceCategoryRequest request) {
    salonAccess.requireOwned(salonId, principal);
    ServiceCategory category = requireCategory(salonId, categoryId);
    category.setName(request.name().trim());
    category.setSortOrder(request.sortOrder());
    return mapper.toDto(category);
  }

  /** Deletes a category; its services keep existing without a category. */
  public void deleteCategory(Long salonId, Long categoryId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    categoryRepository.delete(requireCategory(salonId, categoryId));
  }

  // ---- Owner: services ------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ServiceResponse> listServices(Long salonId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    return mapper.toDtos(serviceRepository.findAllBySalonIdOrderBySortOrderAscNameAsc(salonId));
  }

  public ServiceResponse createService(
      Long salonId, UserPrincipal principal, ServiceRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    SalonService service =
        SalonService.create(
            salon, request.name().trim(), request.durationMin(), request.priceMinor());
    apply(service, salonId, request);
    return mapper.toDto(serviceRepository.save(service));
  }

  public ServiceResponse updateService(
      Long salonId, Long serviceId, UserPrincipal principal, ServiceRequest request) {
    salonAccess.requireOwned(salonId, principal);
    SalonService service = requireService(salonId, serviceId);
    service.setName(request.name().trim());
    service.setDurationMin(request.durationMin());
    service.setPriceMinor(request.priceMinor());
    apply(service, salonId, request);
    return mapper.toDto(service);
  }

  /** Soft-deletes a service so historical bookings keep their reference. */
  public void deleteService(Long salonId, Long serviceId, UserPrincipal principal) {
    salonAccess.requireOwned(salonId, principal);
    SalonService service = requireService(salonId, serviceId);
    service.setActive(false);
    service.setDeletedAt(Instant.now());
  }

  // ---- helpers --------------------------------------------------------------------------------

  private void apply(SalonService service, Long salonId, ServiceRequest request) {
    service.setCategory(
        request.categoryId() == null ? null : requireCategory(salonId, request.categoryId()));
    service.setDescription(request.description());
    service.setBufferAfterMin(request.bufferAfterMin());
    service.setImageUrl(request.imageUrl());
    service.setActive(request.active());
    service.setSortOrder(request.sortOrder());
  }

  private ServiceCategory requireCategory(Long salonId, Long categoryId) {
    return categoryRepository
        .findByIdAndSalonId(categoryId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Service category", categoryId));
  }

  private SalonService requireService(Long salonId, Long serviceId) {
    return serviceRepository
        .findByIdAndSalonId(serviceId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));
  }
}
