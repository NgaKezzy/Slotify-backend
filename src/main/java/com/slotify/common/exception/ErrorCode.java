package com.slotify.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogue of every error the API can return.
 *
 * <p>Each code maps to an HTTP status and to a message key in {@code messages*.properties}, so that
 * clients can branch on {@link #name()} while users see a localised message. Add new codes here
 * rather than throwing ad-hoc exceptions.
 */
public enum ErrorCode {
  // ---- Generic -----------------------------------------------------------
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "error.validation_failed"),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "error.bad_request"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.unauthorized"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "error.forbidden"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "error.not_found"),
  CONFLICT(HttpStatus.CONFLICT, "error.conflict"),
  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "error.too_many_requests"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"),

  // ---- Auth --------------------------------------------------------------
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.auth.invalid_credentials"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "error.auth.token_expired"),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "error.auth.token_invalid"),
  EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "error.auth.email_already_used"),
  EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "error.auth.email_not_verified"),
  ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "error.auth.account_suspended"),

  // ---- Booking -----------------------------------------------------------
  SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "error.booking.slot_unavailable"),
  BOOKING_INVALID_STATE(HttpStatus.CONFLICT, "error.booking.invalid_state"),
  BOOKING_CANCEL_TOO_LATE(HttpStatus.CONFLICT, "error.booking.cancel_too_late"),
  STAFF_CANNOT_PERFORM_SERVICE(HttpStatus.BAD_REQUEST, "error.booking.staff_cannot_perform"),

  // ---- Payment / promotion ----------------------------------------------
  PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "error.payment.failed"),
  PAYMENT_METHOD_NOT_ACCEPTED(HttpStatus.BAD_REQUEST, "error.payment.method_not_accepted"),
  COUPON_INVALID(HttpStatus.BAD_REQUEST, "error.coupon.invalid");

  private final HttpStatus status;
  private final String messageKey;

  ErrorCode(HttpStatus status, String messageKey) {
    this.status = status;
    this.messageKey = messageKey;
  }

  /** HTTP status returned for this error. */
  public HttpStatus status() {
    return status;
  }

  /** Key of the localised message in {@code messages*.properties}. */
  public String messageKey() {
    return messageKey;
  }
}
