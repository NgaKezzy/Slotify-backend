package com.slotify.module.customer.mapper;

import com.slotify.module.customer.dto.CustomerNoteResponse;
import com.slotify.module.customer.dto.CustomerStatsRow;
import com.slotify.module.customer.dto.CustomerSummaryResponse;
import com.slotify.module.customer.dto.CustomerTagResponse;
import com.slotify.module.customer.entity.CustomerNote;
import com.slotify.module.customer.entity.CustomerTag;
import java.util.List;
import org.springframework.stereotype.Component;

/** Builds CRM response records from the stats projection, tags and notes. */
@Component
public class CustomerMapper {

  /** Combines the aggregated statistics with the customer's tags. */
  public CustomerSummaryResponse toSummary(CustomerStatsRow row, List<CustomerTag> tags) {
    return new CustomerSummaryResponse(
        row.customerId(),
        row.fullName(),
        row.email(),
        row.phone(),
        row.avatarUrl(),
        zeroIfNull(row.totalBookings()),
        zeroIfNull(row.completedBookings()),
        zeroIfNull(row.noShows()),
        zeroIfNull(row.cancelledBookings()),
        zeroIfNull(row.totalSpentMinor()),
        row.firstVisitAt(),
        row.lastVisitAt(),
        tags.stream().map(this::toTag).toList());
  }

  public CustomerTagResponse toTag(CustomerTag tag) {
    return new CustomerTagResponse(tag.getId(), tag.getName(), tag.getColor());
  }

  public CustomerNoteResponse toNote(CustomerNote note) {
    return new CustomerNoteResponse(
        note.getId(),
        note.getNote(),
        note.getAuthor().getId(),
        note.getAuthor().getFullName(),
        note.getCreatedAt());
  }

  private static long zeroIfNull(Long value) {
    return value == null ? 0 : value;
  }
}
