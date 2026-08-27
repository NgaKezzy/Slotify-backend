package com.slotify.module.review.mapper;

import com.slotify.module.review.dto.ReviewResponse;
import com.slotify.module.review.entity.Review;
import com.slotify.module.staff.entity.Staff;
import org.springframework.stereotype.Component;

/**
 * Builds {@link ReviewResponse}s. Hand-written because the customer's name is abbreviated on the
 * public salon page and shown in full to the author and the salon owner.
 */
@Component
public class ReviewMapper {

  /** View for the author and the salon owner (full customer name). */
  public ReviewResponse forOwner(Review review) {
    return build(review, review.getCustomer().getFullName());
  }

  /** View for the public salon page (first name plus last initial, e.g. "Chris C."). */
  public ReviewResponse forPublic(Review review) {
    return build(review, abbreviate(review.getCustomer().getFullName()));
  }

  /**
   * Shortens a full name to first name and last-name initial. "Chris Customer" becomes "Chris C.";
   * a single-word name is returned unchanged.
   */
  static String abbreviate(String fullName) {
    String[] parts = fullName == null ? new String[0] : fullName.trim().split("\\s+");
    if (parts.length == 0 || parts[0].isEmpty()) {
      return "";
    }
    if (parts.length == 1) {
      return parts[0];
    }
    return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
  }

  private ReviewResponse build(Review r, String customerName) {
    Staff staff = r.getStaff();
    return new ReviewResponse(
        r.getId(),
        r.getBooking().getId(),
        r.getBooking().getCode(),
        r.getSalon().getId(),
        staff == null ? null : new ReviewResponse.StaffRef(staff.getId(), staff.getDisplayName()),
        customerName,
        r.getRating(),
        r.getComment(),
        r.getReply(),
        r.getRepliedAt(),
        r.isVisible(),
        r.getCreatedAt());
  }
}
