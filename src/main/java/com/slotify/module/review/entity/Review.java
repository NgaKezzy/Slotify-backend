package com.slotify.module.review.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A customer's rating of one completed booking (table {@code reviews}).
 *
 * <p>Exactly one review exists per booking ({@code booking_id} is unique). The salon owner may
 * reply once and hide a review; hidden reviews are excluded from the public list and from the
 * aggregated {@code rating_avg / rating_count} of the salon and staff member.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

  /** Lowest allowed rating. */
  public static final int MIN_RATING = 1;

  /** Highest allowed rating. */
  public static final int MAX_RATING = 5;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  /** Staff member who performed the booking; {@code null} once the staff row was deleted. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "staff_id")
  private Staff staff;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(nullable = false)
  private int rating;

  @Column(columnDefinition = "TEXT")
  private String comment;

  @Column(columnDefinition = "TEXT")
  private String reply;

  @Column(name = "replied_at")
  private Instant repliedAt;

  @Column(name = "is_visible", nullable = false)
  private boolean visible = true;

  /** Creates a visible review for the booking, copying salon, staff and customer from it. */
  public static Review create(Booking booking, int rating, String comment) {
    Review review = new Review();
    review.booking = booking;
    review.salon = booking.getSalon();
    review.staff = booking.getStaff();
    review.customer = booking.getCustomer();
    review.rating = rating;
    review.comment = comment;
    return review;
  }

  /** Stores the owner's reply and the time it was written. */
  public void reply(String text, Instant at) {
    this.reply = text;
    this.repliedAt = at;
  }
}
