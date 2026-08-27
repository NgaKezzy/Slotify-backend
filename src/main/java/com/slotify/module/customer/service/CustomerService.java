package com.slotify.module.customer.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.mapper.BookingMapper;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.customer.dto.CustomerDetailResponse;
import com.slotify.module.customer.dto.CustomerNoteResponse;
import com.slotify.module.customer.dto.CustomerStatsRow;
import com.slotify.module.customer.dto.CustomerSummaryResponse;
import com.slotify.module.customer.dto.CustomerTagResponse;
import com.slotify.module.customer.entity.CustomerNote;
import com.slotify.module.customer.entity.CustomerTag;
import com.slotify.module.customer.entity.CustomerTagLink;
import com.slotify.module.customer.mapper.CustomerMapper;
import com.slotify.module.customer.repository.CustomerNoteRepository;
import com.slotify.module.customer.repository.CustomerStatsRepository;
import com.slotify.module.customer.repository.CustomerTagLinkRepository;
import com.slotify.module.customer.repository.CustomerTagRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import com.slotify.security.UserPrincipal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRM of a salon (owner only): customer list with visit statistics, detail page, internal notes and
 * tag assignment.
 *
 * <p>A user counts as a customer of the salon as soon as they have one booking there; ids of other
 * users are rejected with {@link ErrorCode#USER_NOT_FOUND} so owners cannot probe accounts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

  private static final int MAX_PAGE_SIZE = 50;
  private static final int RECENT_BOOKINGS = 10;

  private final CustomerStatsRepository statsRepository;
  private final CustomerNoteRepository noteRepository;
  private final CustomerTagRepository tagRepository;
  private final CustomerTagLinkRepository linkRepository;
  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final SalonAccess salonAccess;
  private final BookingMapper bookingMapper;
  private final CustomerMapper mapper;

  /** Customers of the salon (optionally filtered by name / email), most recent booking first. */
  @Transactional(readOnly = true)
  public PageResponse<CustomerSummaryResponse> list(
      UserPrincipal principal, Long salonId, String q, int page, int size) {
    salonAccess.requireOwned(salonId, principal);
    PageRequest pageable = PageRequest.of(page, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    Page<CustomerStatsRow> rows =
        q == null || q.isBlank()
            ? statsRepository.findAll(
                salonId,
                BookingStatus.COMPLETED,
                BookingStatus.NO_SHOW,
                BookingStatus.CANCELLED,
                pageable)
            : statsRepository.search(
                salonId,
                "%" + q.trim().toLowerCase() + "%",
                BookingStatus.COMPLETED,
                BookingStatus.NO_SHOW,
                BookingStatus.CANCELLED,
                pageable);
    Map<Long, List<CustomerTag>> tags = tagsByCustomer(salonId, rows.getContent());
    return PageResponse.from(
        rows, row -> mapper.toSummary(row, tags.getOrDefault(row.customerId(), List.of())));
  }

  /** Statistics, contact, tags, notes and the last bookings of one customer. */
  @Transactional(readOnly = true)
  public CustomerDetailResponse get(UserPrincipal principal, Long salonId, Long customerId) {
    salonAccess.requireOwned(salonId, principal);
    CustomerStatsRow row = requireStats(salonId, customerId);
    CustomerSummaryResponse summary =
        mapper.toSummary(row, tagRepository.findAssigned(salonId, customerId));
    List<CustomerNoteResponse> notes =
        noteRepository
            .findAllBySalonIdAndCustomerIdOrderByCreatedAtDesc(salonId, customerId)
            .stream()
            .map(mapper::toNote)
            .toList();
    var recent =
        bookingRepository
            .findAllBySalonIdAndCustomerIdOrderByStartAtDesc(
                salonId, customerId, PageRequest.of(0, RECENT_BOOKINGS))
            .stream()
            .map(bookingMapper::forSalon)
            .toList();
    return new CustomerDetailResponse(summary, notes, recent);
  }

  // ---- Notes ---------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<CustomerNoteResponse> listNotes(
      UserPrincipal principal, Long salonId, Long customerId) {
    salonAccess.requireOwned(salonId, principal);
    requireStats(salonId, customerId);
    return noteRepository
        .findAllBySalonIdAndCustomerIdOrderByCreatedAtDesc(salonId, customerId)
        .stream()
        .map(mapper::toNote)
        .toList();
  }

  /** Adds a note written by the caller. */
  public CustomerNoteResponse addNote(
      UserPrincipal principal, Long salonId, Long customerId, String text) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    requireStats(salonId, customerId);
    User customer = requireUser(customerId);
    User author = requireUser(principal.id());
    CustomerNote note = noteRepository.save(CustomerNote.create(salon, customer, author, text));
    return mapper.toNote(note);
  }

  /**
   * Deletes a note. Everyone who may manage the salon (owner / super admin) can delete any note of
   * it, which includes the note's author.
   */
  public void deleteNote(UserPrincipal principal, Long salonId, Long customerId, Long noteId) {
    salonAccess.requireOwned(salonId, principal);
    CustomerNote note =
        noteRepository
            .findByIdAndSalonIdAndCustomerId(noteId, salonId, customerId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Note", noteId));
    noteRepository.delete(note);
  }

  // ---- Tags ----------------------------------------------------------------------------------

  /**
   * Replaces the salon's tags of a customer with {@code tagIds}.
   *
   * @throws AppException {@link ErrorCode#NOT_FOUND} when a tag does not belong to the salon
   */
  public List<CustomerTagResponse> assignTags(
      UserPrincipal principal, Long salonId, Long customerId, List<Long> tagIds) {
    salonAccess.requireOwned(salonId, principal);
    requireStats(salonId, customerId);
    List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(tagIds));
    if (!distinct.isEmpty()
        && tagRepository.countBySalonIdAndIdIn(salonId, distinct) != distinct.size()) {
      throw new AppException(ErrorCode.NOT_FOUND, "Tag", distinct);
    }
    linkRepository.deleteAllForSalon(salonId, customerId);
    linkRepository.saveAll(
        distinct.stream().map(id -> CustomerTagLink.of(customerId, id)).toList());
    linkRepository.flush();
    return tagRepository.findAssigned(salonId, customerId).stream().map(mapper::toTag).toList();
  }

  // ---- Helpers -------------------------------------------------------------------------------

  private CustomerStatsRow requireStats(Long salonId, Long customerId) {
    return statsRepository
        .findOne(
            salonId,
            customerId,
            BookingStatus.COMPLETED,
            BookingStatus.NO_SHOW,
            BookingStatus.CANCELLED)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private User requireUser(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private Map<Long, List<CustomerTag>> tagsByCustomer(Long salonId, List<CustomerStatsRow> rows) {
    Map<Long, List<CustomerTag>> result = new HashMap<>();
    if (rows.isEmpty()) {
      return result;
    }
    List<Long> ids = rows.stream().map(CustomerStatsRow::customerId).toList();
    for (Object[] pair : tagRepository.findAssignedForCustomers(salonId, ids)) {
      result.computeIfAbsent((Long) pair[0], k -> new ArrayList<>()).add((CustomerTag) pair[1]);
    }
    return result;
  }
}
