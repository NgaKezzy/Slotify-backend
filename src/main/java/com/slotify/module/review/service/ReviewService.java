package com.slotify.module.review.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.review.dto.CreateReviewRequest;
import com.slotify.module.review.dto.ReviewResponse;
import com.slotify.module.review.dto.ReviewSummaryResponse;
import com.slotify.module.review.entity.Review;
import com.slotify.module.review.mapper.ReviewMapper;
import com.slotify.module.review.repository.ReviewRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.module.salon.service.SalonQueryService;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Review use cases: customers rate a completed booking once, everybody reads visible reviews of a
 * salon, owners reply and hide. Every change that affects visibility re-aggregates the salon and
 * staff ratings through {@link RatingAggregator}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

  private static final int MAX_PAGE_SIZE = 50;

  private final ReviewRepository reviewRepository;
  private final BookingRepository bookingRepository;
  private final SalonAccess salonAccess;
  private final SalonQueryService salonQueryService;
  private final RatingAggregator ratingAggregator;
  private final ReviewEvents events;
  private final ReviewMapper mapper;
  private final Clock clock;

  // ---- Customer ------------------------------------------------------------------------------

  /**
   * Writes the review of one of the caller's bookings.
   *
   * @throws AppException {@link ErrorCode#BOOKING_NOT_FOUND}, {@link ErrorCode#REVIEW_NOT_ALLOWED}
   *     when the booking is not completed, {@link ErrorCode#REVIEW_ALREADY_EXISTS}
   */
  public ReviewResponse create(UserPrincipal principal, Long bookingId, CreateReviewRequest req) {
    Booking booking = requireMine(principal, bookingId);
    if (booking.getStatus() != BookingStatus.COMPLETED) {
      throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
    }
    if (reviewRepository.existsByBookingId(booking.getId())) {
      throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }
    Review review = reviewRepository.save(Review.create(booking, req.rating(), req.comment()));
    reviewRepository.flush();
    ratingAggregator.refresh(review.getSalon(), review.getStaff());
    events.created(review);
    return mapper.forOwner(review);
  }

  /** The caller's review of one of their bookings. */
  @Transactional(readOnly = true)
  public ReviewResponse getMine(UserPrincipal principal, Long bookingId) {
    Booking booking = requireMine(principal, bookingId);
    return reviewRepository
        .findByBookingId(booking.getId())
        .map(mapper::forOwner)
        .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
  }

  // ---- Public --------------------------------------------------------------------------------

  /** Visible reviews of an active salon, newest first, with abbreviated customer names. */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> listPublic(String idOrSlug, int page, int size) {
    Salon salon = salonQueryService.requireActive(idOrSlug);
    Page<Review> result =
        reviewRepository.findAllBySalonIdAndVisibleTrue(salon.getId(), pageable(page, size));
    return PageResponse.from(result, mapper::forPublic);
  }

  /** Average, count and 1..5 distribution of the visible reviews of an active salon. */
  @Transactional(readOnly = true)
  public ReviewSummaryResponse summary(String idOrSlug) {
    Salon salon = salonQueryService.requireActive(idOrSlug);
    Map<Integer, Long> distribution = new LinkedHashMap<>();
    for (int rating = Review.MIN_RATING; rating <= Review.MAX_RATING; rating++) {
      distribution.put(rating, 0L);
    }
    long count = 0;
    long sum = 0;
    for (Object[] row : reviewRepository.distributionForSalon(salon.getId())) {
      int rating = ((Number) row[0]).intValue();
      long n = ((Number) row[1]).longValue();
      distribution.put(rating, n);
      count += n;
      sum += (long) rating * n;
    }
    Double average = count == 0 ? null : (double) sum / count;
    return new ReviewSummaryResponse(RatingAggregator.average(average), count, distribution);
  }

  // ---- Owner ---------------------------------------------------------------------------------

  /** Reviews of an owned salon, optionally filtered by visibility. */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> listForSalon(
      UserPrincipal principal, Long salonId, Boolean visible, int page, int size) {
    salonAccess.requireOwned(salonId, principal);
    Page<Review> result =
        visible == null
            ? reviewRepository.findAllBySalonId(salonId, pageable(page, size))
            : reviewRepository.findAllBySalonIdAndVisible(salonId, visible, pageable(page, size));
    return PageResponse.from(result, mapper::forOwner);
  }

  /** Stores (or replaces) the owner's public reply. */
  public ReviewResponse reply(UserPrincipal principal, Long salonId, Long reviewId, String text) {
    Review review = requireForSalon(principal, salonId, reviewId);
    review.reply(text, Instant.now(clock));
    return mapper.forOwner(review);
  }

  /** Shows or hides a review and re-aggregates the salon / staff ratings. */
  public ReviewResponse setVisibility(
      UserPrincipal principal, Long salonId, Long reviewId, boolean visible) {
    Review review = requireForSalon(principal, salonId, reviewId);
    if (review.isVisible() != visible) {
      review.setVisible(visible);
      reviewRepository.flush();
      ratingAggregator.refresh(review.getSalon(), review.getStaff());
    }
    return mapper.forOwner(review);
  }

  // ---- Helpers -------------------------------------------------------------------------------

  private static PageRequest pageable(int page, int size) {
    return PageRequest.of(
        page, Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("createdAt").descending());
  }

  private Booking requireMine(UserPrincipal principal, Long bookingId) {
    return bookingRepository
        .findByIdAndCustomerId(bookingId, principal.id())
        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
  }

  private Review requireForSalon(UserPrincipal principal, Long salonId, Long reviewId) {
    salonAccess.requireOwned(salonId, principal);
    return reviewRepository
        .findByIdAndSalonId(reviewId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
  }
}
