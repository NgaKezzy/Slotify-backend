package com.slotify.module.salon.mapper;

import com.slotify.module.salon.dto.AmenityResponse;
import com.slotify.module.salon.dto.CategoryResponse;
import com.slotify.module.salon.dto.OpeningHourDto;
import com.slotify.module.salon.dto.SalonDetailResponse;
import com.slotify.module.salon.dto.SalonSettingsDto;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.entity.Amenity;
import com.slotify.module.salon.entity.Category;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonImage;
import com.slotify.module.salon.entity.SalonOpeningHour;
import com.slotify.module.salon.entity.SalonSettings;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper for the salon module. */
@Mapper
public interface SalonMapper {

  @Mapping(target = "categoryIds", expression = "java(categoryIds(salon))")
  @Mapping(target = "distanceKm", ignore = true)
  SalonSummaryResponse toSummary(Salon salon);

  @Mapping(target = "ownerId", source = "owner.id")
  @Mapping(target = "images", expression = "java(imageUrls(salon))")
  SalonDetailResponse toDetail(Salon salon);

  @Mapping(target = "day", expression = "java(hour.day())")
  OpeningHourDto toDto(SalonOpeningHour hour);

  SalonSettingsDto toDto(SalonSettings settings);

  CategoryResponse toDto(Category category);

  AmenityResponse toDto(Amenity amenity);

  List<CategoryResponse> toCategoryDtos(List<Category> categories);

  List<AmenityResponse> toAmenityDtos(List<Amenity> amenities);

  default List<Long> categoryIds(Salon salon) {
    return salon.getCategories().stream().map(Category::getId).toList();
  }

  default List<String> imageUrls(Salon salon) {
    return salon.getImages().stream().map(SalonImage::getUrl).toList();
  }
}
