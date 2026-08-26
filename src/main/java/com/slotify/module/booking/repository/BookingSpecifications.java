package com.slotify.module.booking.repository;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filters for the owner's booking list. */
public final class BookingSpecifications {

  private BookingSpecifications() {}

  /** Builds the combined specification; null filters are ignored. */
  public static Specification<Booking> filter(
      Long salonId, BookingStatus status, Long staffId, Instant from, Instant to, String q) {
    List<Specification<Booking>> specs = new ArrayList<>();
    specs.add((root, query, cb) -> cb.equal(root.get("salon").get("id"), salonId));
    if (status != null) {
      specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (staffId != null) {
      specs.add((root, query, cb) -> cb.equal(root.get("staff").get("id"), staffId));
    }
    if (from != null) {
      specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), from));
    }
    if (to != null) {
      specs.add((root, query, cb) -> cb.lessThan(root.get("startAt"), to));
    }
    if (q != null && !q.isBlank()) {
      String pattern = "%" + q.trim().toLowerCase() + "%";
      specs.add(
          (root, query, cb) ->
              cb.or(
                  cb.like(cb.lower(root.get("code")), pattern),
                  cb.like(cb.lower(root.get("customer").get("fullName")), pattern),
                  cb.like(cb.lower(root.get("customer").get("email")), pattern)));
    }
    return Specification.allOf(specs);
  }
}
