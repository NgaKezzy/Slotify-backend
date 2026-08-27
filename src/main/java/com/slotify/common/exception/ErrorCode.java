package com.slotify.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogue of every business error the API can return, exposed as the numeric {@code code} of
 * {@code ApiResponse}.
 *
 * <p>Code ranges:
 *
 * <ul>
 *   <li>{@code 1000} – success (see {@code ApiResponse.SUCCESS_CODE})
 *   <li>{@code 1xxx} – authentication &amp; permission
 *   <li>{@code 2xxx} – resource not found
 *   <li>{@code 3xxx} – invalid request
 *   <li>{@code 4xxx} – data / state conflict
 *   <li>{@code 5xxx} – payment
 *   <li>{@code 9xxx} – system / uncategorised
 * </ul>
 *
 * <p>Each entry carries the HTTP status, a message key resolved through {@code MessageSource}
 * ({@code i18n/messages*.properties}) and an English fallback used when the key is missing. Clients
 * branch on the numeric code; the Flutter {@code ErrorCode} enum mirrors these values.
 */
public enum ErrorCode {
  // ---- 1xxx: auth & permission ------------------------------------------
  UNAUTHENTICATED(
      1001, HttpStatus.UNAUTHORIZED, "error.unauthenticated", "Authentication required"),
  FORBIDDEN(1002, HttpStatus.FORBIDDEN, "error.forbidden", "Permission denied"),
  INVALID_CREDENTIALS(
      1003, HttpStatus.UNAUTHORIZED, "error.auth.invalid_credentials", "Invalid email or password"),
  TOKEN_EXPIRED(1004, HttpStatus.UNAUTHORIZED, "error.auth.token_expired", "Session expired"),
  TOKEN_INVALID(1005, HttpStatus.UNAUTHORIZED, "error.auth.token_invalid", "Invalid token"),
  EMAIL_NOT_VERIFIED(
      1006, HttpStatus.FORBIDDEN, "error.auth.email_not_verified", "Email not verified"),
  ACCOUNT_SUSPENDED(
      1007, HttpStatus.FORBIDDEN, "error.auth.account_suspended", "Account suspended"),
  NOT_STAFF_ACCOUNT(
      1008, HttpStatus.FORBIDDEN, "error.auth.not_staff_account", "Not a staff account"),

  // ---- 2xxx: not found ---------------------------------------------------
  NOT_FOUND(2001, HttpStatus.NOT_FOUND, "error.not_found", "Resource not found"),
  USER_NOT_FOUND(2002, HttpStatus.NOT_FOUND, "error.user.not_found", "User not found"),
  SALON_NOT_FOUND(2003, HttpStatus.NOT_FOUND, "error.salon.not_found", "Salon not found"),
  SERVICE_NOT_FOUND(2004, HttpStatus.NOT_FOUND, "error.service.not_found", "Service not found"),
  STAFF_NOT_FOUND(2005, HttpStatus.NOT_FOUND, "error.staff.not_found", "Staff member not found"),
  BOOKING_NOT_FOUND(2006, HttpStatus.NOT_FOUND, "error.booking.not_found", "Booking not found"),
  COUPON_NOT_FOUND(2007, HttpStatus.NOT_FOUND, "error.coupon.not_found", "Coupon not found"),

  // ---- 3xxx: invalid request --------------------------------------------
  VALIDATION_FAILED(3001, HttpStatus.BAD_REQUEST, "error.validation_failed", "Validation failed"),
  BAD_REQUEST(3002, HttpStatus.BAD_REQUEST, "error.bad_request", "Bad request"),
  STAFF_CANNOT_PERFORM_SERVICE(
      3003,
      HttpStatus.BAD_REQUEST,
      "error.booking.staff_cannot_perform",
      "Staff member does not offer this service"),
  PAYMENT_METHOD_NOT_ACCEPTED(
      3004,
      HttpStatus.BAD_REQUEST,
      "error.payment.method_not_accepted",
      "Payment method not accepted"),
  COUPON_INVALID(3005, HttpStatus.BAD_REQUEST, "error.coupon.invalid", "Coupon is not valid"),
  TOO_MANY_REQUESTS(
      3006, HttpStatus.TOO_MANY_REQUESTS, "error.too_many_requests", "Too many requests"),
  INVALID_TIME_RANGE(
      3007, HttpStatus.BAD_REQUEST, "error.invalid_time_range", "End must be after start"),

  // ---- 4xxx: conflict ----------------------------------------------------
  CONFLICT(4001, HttpStatus.CONFLICT, "error.conflict", "Conflict"),
  EMAIL_ALREADY_USED(
      4002, HttpStatus.CONFLICT, "error.auth.email_already_used", "Email already registered"),
  SLOT_UNAVAILABLE(
      4003, HttpStatus.CONFLICT, "error.booking.slot_unavailable", "Time slot unavailable"),
  BOOKING_INVALID_STATE(
      4004, HttpStatus.CONFLICT, "error.booking.invalid_state", "Invalid booking state"),
  BOOKING_CANCEL_TOO_LATE(
      4005,
      HttpStatus.CONFLICT,
      "error.booking.cancel_too_late",
      "Booking can no longer be cancelled"),
  SHIFT_OVERLAP(4006, HttpStatus.CONFLICT, "error.staff.shift_overlap", "Shift windows overlap"),
  LAST_SUPER_ADMIN(
      4007,
      HttpStatus.CONFLICT,
      "error.user.last_super_admin",
      "The last super admin cannot be demoted or suspended"),

  // ---- 5xxx: payment -----------------------------------------------------
  PAYMENT_FAILED(5001, HttpStatus.BAD_REQUEST, "error.payment.failed", "Payment failed"),

  // ---- 9xxx: system ------------------------------------------------------
  INTERNAL_ERROR(
      9999, HttpStatus.INTERNAL_SERVER_ERROR, "error.internal", "Unexpected server error");

  private final int code;
  private final HttpStatus status;
  private final String messageKey;
  private final String defaultMessage;

  ErrorCode(int code, HttpStatus status, String messageKey, String defaultMessage) {
    this.code = code;
    this.status = status;
    this.messageKey = messageKey;
    this.defaultMessage = defaultMessage;
  }

  /** Numeric code sent to clients in {@code ApiResponse.code}. */
  public int code() {
    return code;
  }

  /** HTTP status returned for this error. */
  public HttpStatus status() {
    return status;
  }

  /** Key of the localised message in {@code messages*.properties}. */
  public String messageKey() {
    return messageKey;
  }

  /** English fallback shown when no localised message exists. */
  public String defaultMessage() {
    return defaultMessage;
  }
}
