package com.slotify.module.staff.entity;

import com.slotify.common.entity.SoftDeletableEntity;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * An employee of a salon who performs services and can be booked (table {@code staffs}).
 *
 * <p>A staff member optionally links to a login account ({@code user_id}, role {@code STAFF}) once
 * the owner invites them; unlinked staff can still be scheduled and booked. The services a staff
 * member can perform are stored in {@code staff_services}; weekly availability lives in {@link
 * StaffShift}, per-day exceptions in {@link StaffShiftOverride} and {@link StaffTimeOff}.
 */
@Entity
@Table(name = "staffs")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff extends SoftDeletableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  /** Linked login account; {@code null} until the owner has invited the staff member. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "display_name", nullable = false, length = 150)
  private String displayName;

  /** Job title shown to customers, e.g. "Senior Stylist". */
  @Column(length = 100)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
  private BigDecimal ratingAvg = BigDecimal.ZERO;

  @Column(name = "rating_count", nullable = false)
  private int ratingCount;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "staff_services",
      joinColumns = @JoinColumn(name = "staff_id"),
      inverseJoinColumns = @JoinColumn(name = "service_id"))
  private Set<SalonService> services = new LinkedHashSet<>();

  /** Creates an active staff member of the given salon without a linked account. */
  public static Staff create(Salon salon, String displayName) {
    Staff staff = new Staff();
    staff.salon = salon;
    staff.displayName = displayName;
    return staff;
  }

  /** Replaces the set of services this staff member can perform. */
  public void replaceServices(Collection<SalonService> newServices) {
    services.clear();
    services.addAll(newServices);
  }
}
