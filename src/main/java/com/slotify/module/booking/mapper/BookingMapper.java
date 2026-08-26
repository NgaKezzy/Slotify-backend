package com.slotify.module.booking.mapper;

import com.slotify.module.booking.dto.BookingResponse;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Builds {@link BookingResponse}s. Hand-written (not MapStruct) because the shape depends on who is
 * asking: customer contact details are only included for salon-side viewers.
 */
@Component
public class BookingMapper {

  /** View for the customer who owns the booking (no contact block). */
  public BookingResponse forCustomer(Booking booking) {
    return build(booking, false);
  }

  /** View for owners and staff (includes customer contact). */
  public BookingResponse forSalon(Booking booking) {
    return build(booking, true);
  }

  private BookingResponse build(Booking b, boolean includeCustomer) {
    Salon salon = b.getSalon();
    Staff staff = b.getStaff();
    User customer = b.getCustomer();
    return new BookingResponse(
        b.getId(),
        b.getCode(),
        new BookingResponse.SalonRef(
            salon.getId(),
            salon.getName(),
            salon.getSlug(),
            salon.getAddress(),
            salon.getTimezone()),
        staff == null
            ? null
            : new BookingResponse.StaffRef(
                staff.getId(), staff.getDisplayName(), staff.getAvatarUrl()),
        b.isStaffAutoAssigned(),
        includeCustomer
            ? new BookingResponse.CustomerRef(
                customer.getId(), customer.getFullName(), customer.getEmail(), customer.getPhone())
            : null,
        b.getItems().stream().map(this::item).toList(),
        b.getStartAt(),
        b.getEndAt(),
        b.getStatus(),
        b.getSubtotalMinor(),
        b.getDiscountMinor(),
        b.getTotalMinor(),
        b.getCurrency(),
        b.getPaymentStatus(),
        b.getPaymentMethod(),
        b.getNote(),
        b.getCancelReason(),
        b.getCancelledBy(),
        b.getCreatedAt());
  }

  private BookingResponse.Item item(BookingItem item) {
    return new BookingResponse.Item(
        item.getService() == null ? null : item.getService().getId(),
        item.getServiceName(),
        item.getDurationMin(),
        item.getPriceMinor());
  }
}
