package com.slotify.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Standard envelope for every JSON response of the API.
 *
 * <pre>{@code
 * { "success": true,  "data": {...}, "message": "Booking created" }
 * { "success": false, "message": "Validation failed", "errors": [ {...} ] }
 * }</pre>
 *
 * @param success whether the request was handled successfully
 * @param data response payload (omitted when null)
 * @param message optional human-readable message, already localised
 * @param errors error details for failed requests (omitted when empty)
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, List<ApiError> errors) {

  /** Successful response with a payload. */
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  /** Successful response with a payload and a message. */
  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, data, message, null);
  }

  /** Successful response without a payload. */
  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(true, null, null, null);
  }

  /** Failed response with a message and optional error details. */
  public static ApiResponse<Void> error(String message, List<ApiError> errors) {
    return new ApiResponse<>(
        false, null, message, errors == null || errors.isEmpty() ? null : errors);
  }
}
