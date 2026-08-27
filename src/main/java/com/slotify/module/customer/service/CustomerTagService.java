package com.slotify.module.customer.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.customer.dto.CustomerTagRequest;
import com.slotify.module.customer.dto.CustomerTagResponse;
import com.slotify.module.customer.entity.CustomerTag;
import com.slotify.module.customer.mapper.CustomerMapper;
import com.slotify.module.customer.repository.CustomerTagRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD of the tag catalogue of a salon (owner only). Assigning tags lives in {@link
 * CustomerService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerTagService {

  private final CustomerTagRepository tagRepository;
  private final SalonAccess salonAccess;
  private final CustomerMapper mapper;

  @Transactional(readOnly = true)
  public List<CustomerTagResponse> list(UserPrincipal principal, Long salonId) {
    salonAccess.requireOwned(salonId, principal);
    return tagRepository.findAllBySalonIdOrderByNameAsc(salonId).stream()
        .map(mapper::toTag)
        .toList();
  }

  /**
   * Creates a tag.
   *
   * @throws AppException {@link ErrorCode#CUSTOMER_TAG_ALREADY_EXISTS} when the name is taken
   */
  public CustomerTagResponse create(
      UserPrincipal principal, Long salonId, CustomerTagRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    String name = request.name().trim();
    if (tagRepository.existsBySalonIdAndNameIgnoreCase(salonId, name)) {
      throw new AppException(ErrorCode.CUSTOMER_TAG_ALREADY_EXISTS);
    }
    return mapper.toTag(tagRepository.save(CustomerTag.create(salon, name, request.color())));
  }

  /** Renames / recolours a tag. */
  public CustomerTagResponse update(
      UserPrincipal principal, Long salonId, Long tagId, CustomerTagRequest request) {
    CustomerTag tag = require(principal, salonId, tagId);
    String name = request.name().trim();
    if (tagRepository.existsBySalonIdAndNameIgnoreCaseAndIdNot(salonId, name, tagId)) {
      throw new AppException(ErrorCode.CUSTOMER_TAG_ALREADY_EXISTS);
    }
    tag.setName(name);
    if (request.color() != null) {
      tag.setColor(request.color());
    }
    return mapper.toTag(tag);
  }

  /** Deletes a tag; its assignments are removed by the database cascade. */
  public void delete(UserPrincipal principal, Long salonId, Long tagId) {
    tagRepository.delete(require(principal, salonId, tagId));
  }

  private CustomerTag require(UserPrincipal principal, Long salonId, Long tagId) {
    salonAccess.requireOwned(salonId, principal);
    return tagRepository
        .findByIdAndSalonId(tagId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Tag", tagId));
  }
}
