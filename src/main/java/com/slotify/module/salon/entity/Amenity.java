package com.slotify.module.salon.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Global amenity (Wi-Fi, Parking, Wheelchair access, ...) a salon can advertise. */
@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Amenity extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  /** Icon identifier understood by the clients (e.g. Material icon name). */
  @Column(length = 100)
  private String icon;

  public static Amenity of(String name, String icon) {
    Amenity amenity = new Amenity();
    amenity.name = name;
    amenity.icon = icon;
    return amenity;
  }
}
