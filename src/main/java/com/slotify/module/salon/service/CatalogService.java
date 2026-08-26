package com.slotify.module.salon.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.salon.dto.AmenityRequest;
import com.slotify.module.salon.dto.AmenityResponse;
import com.slotify.module.salon.dto.CategoryRequest;
import com.slotify.module.salon.dto.CategoryResponse;
import com.slotify.module.salon.entity.Amenity;
import com.slotify.module.salon.entity.Category;
import com.slotify.module.salon.mapper.SalonMapper;
import com.slotify.module.salon.repository.AmenityRepository;
import com.slotify.module.salon.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Global catalog (categories, amenities) — read by everyone, written by SUPER_ADMIN. */
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {

  private final CategoryRepository categoryRepository;
  private final AmenityRepository amenityRepository;
  private final SalonMapper mapper;

  @Transactional(readOnly = true)
  public List<CategoryResponse> listCategories() {
    return mapper.toCategoryDtos(categoryRepository.findAllByOrderBySortOrderAscNameAsc());
  }

  public CategoryResponse createCategory(CategoryRequest request) {
    return mapper.toDto(
        categoryRepository.save(
            Category.of(request.name().trim(), request.iconUrl(), request.sortOrder())));
  }

  public CategoryResponse updateCategory(Long id, CategoryRequest request) {
    Category category = requireCategory(id);
    category.setName(request.name().trim());
    category.setIconUrl(request.iconUrl());
    category.setSortOrder(request.sortOrder());
    return mapper.toDto(category);
  }

  public void deleteCategory(Long id) {
    categoryRepository.delete(requireCategory(id));
  }

  @Transactional(readOnly = true)
  public List<AmenityResponse> listAmenities() {
    return mapper.toAmenityDtos(amenityRepository.findAllByOrderByNameAsc());
  }

  public AmenityResponse createAmenity(AmenityRequest request) {
    return mapper.toDto(amenityRepository.save(Amenity.of(request.name().trim(), request.icon())));
  }

  public AmenityResponse updateAmenity(Long id, AmenityRequest request) {
    Amenity amenity = requireAmenity(id);
    amenity.setName(request.name().trim());
    amenity.setIcon(request.icon());
    return mapper.toDto(amenity);
  }

  public void deleteAmenity(Long id) {
    amenityRepository.delete(requireAmenity(id));
  }

  private Category requireCategory(Long id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Category", id));
  }

  private Amenity requireAmenity(Long id) {
    return amenityRepository
        .findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Amenity", id));
  }
}
