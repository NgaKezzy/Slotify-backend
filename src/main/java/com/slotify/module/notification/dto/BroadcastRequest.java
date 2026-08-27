package com.slotify.module.notification.dto;

import com.slotify.module.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Body of {@code POST /admin/platform/notifications/broadcast}: a free-text announcement pushed
 * (and stored in the in-app list) for every user of the chosen audience.
 *
 * @param title notification title
 * @param body notification text
 * @param audience who receives it
 */
public record BroadcastRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 500) String body,
    @NotNull Audience audience) {

  /** Recipient groups; the web-only SALON_OWNER / SUPER_ADMIN roles are never targeted. */
  public enum Audience {
    /** Customer app users. */
    CUSTOMERS(List.of(Role.CUSTOMER)),
    /** Staff app users. */
    STAFF(List.of(Role.STAFF)),
    /** Both apps. */
    ALL(List.of(Role.CUSTOMER, Role.STAFF));

    private final List<Role> roles;

    Audience(List<Role> roles) {
      this.roles = roles;
    }

    /** Roles included in this audience. */
    public List<Role> roles() {
      return roles;
    }
  }
}
