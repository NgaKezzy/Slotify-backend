package com.slotify.module.service.mapper;

import com.slotify.module.service.dto.ServiceCategoryResponse;
import com.slotify.module.service.dto.ServiceResponse;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.entity.ServiceCategory;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper for the service module. */
@Mapper
public interface ServiceMapper {

  @Mapping(target = "categoryId", source = "category.id")
  @Mapping(target = "categoryName", source = "category.name")
  ServiceResponse toDto(SalonService service);

  List<ServiceResponse> toDtos(List<SalonService> services);

  ServiceCategoryResponse toDto(ServiceCategory category);

  List<ServiceCategoryResponse> toCategoryDtos(List<ServiceCategory> categories);
}
