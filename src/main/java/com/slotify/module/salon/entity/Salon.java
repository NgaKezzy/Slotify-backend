package com.slotify.module.salon.entity;

import com.slotify.common.entity.SoftDeletableEntity;
import com.slotify.module.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * A salon / spa that customers can book. The tenant root of the multi-salon model: every
 * salon-scoped entity references it through {@code salon_id}.
 */
@Entity
@Table(name = "salons")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Salon extends SoftDeletableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 160)
  private String slug;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(nullable = false, length = 255)
  private String address;

  @Column(nullable = false, length = 100)
  private String city;

  /** ISO-3166-1 alpha-2 country code. */
  @Column(nullable = false, length = 2)
  private String country;

  @Column(precision = 10, scale = 7)
  private BigDecimal lat;

  @Column(precision = 10, scale = 7)
  private BigDecimal lng;

  /** IANA timezone id; opening hours and shifts are expressed in this zone. */
  @Column(nullable = false, length = 64)
  private String timezone = "Europe/Berlin";

  /** ISO-4217 currency of every price in this salon. */
  @Column(nullable = false, length = 3)
  private String currency = "EUR";

  @Column(name = "cover_url", length = 500)
  private String coverUrl;

  @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
  private BigDecimal ratingAvg = BigDecimal.ZERO;

  @Column(name = "rating_count", nullable = false)
  private int ratingCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SalonStatus status = SalonStatus.PENDING;

  /** Platform commission (percent) on online payments; informational in v1. */
  @Column(name = "commission_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal commissionPercent = BigDecimal.ZERO;

  @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC")
  private List<SalonImage> images = new ArrayList<>();

  @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("dayOfWeek ASC")
  private List<SalonOpeningHour> openingHours = new ArrayList<>();

  @OneToOne(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
  private SalonSettings settings;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "salon_categories",
      joinColumns = @JoinColumn(name = "salon_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  private Set<Category> categories = new LinkedHashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "salon_amenities",
      joinColumns = @JoinColumn(name = "salon_id"),
      inverseJoinColumns = @JoinColumn(name = "amenity_id"))
  private Set<Amenity> amenities = new LinkedHashSet<>();

  /** Creates a salon in PENDING state with default settings. */
  public static Salon create(User owner, String name, String slug) {
    Salon salon = new Salon();
    salon.owner = owner;
    salon.name = name;
    salon.slug = slug;
    salon.settings = SalonSettings.defaults(salon);
    return salon;
  }

  public ZoneId zoneId() {
    return ZoneId.of(timezone);
  }

  public boolean isOwnedBy(Long userId) {
    return owner.getId().equals(userId);
  }

  public boolean isActive() {
    return status == SalonStatus.ACTIVE;
  }

  /** Replaces the opening hours (one row per weekday). */
  public void replaceOpeningHours(List<SalonOpeningHour> hours) {
    openingHours.clear();
    hours.forEach(
        h -> {
          h.setSalon(this);
          openingHours.add(h);
        });
  }

  /** Replaces the gallery keeping the given order. */
  public void replaceImages(List<String> urls) {
    images.clear();
    for (int i = 0; i < urls.size(); i++) {
      images.add(SalonImage.of(this, urls.get(i), i));
    }
  }
}
