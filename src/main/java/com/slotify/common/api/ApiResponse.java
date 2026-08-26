package com.slotify.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.slotify.common.exception.ErrorCode;
import java.util.List;

/**
 * Standard envelope for every JSON response of the API.
 *
 * <pre>{@code
 * { "success": true,  "code": 1000, "message": "Booking created", "data": {...} }
 * { "success": false, "code": 4003, "message": "Time slot unavailable", "errors": [ {...} ] }
 * }</pre>
 *
 * <p>Clients branch on {@code code} (never on {@code message}, which is localised). The Flutter app
 * mirrors this shape as {@code BaseResponse<T>}.
 *
 * @param success whether the request was handled successfully
 * @param code {@link #SUCCESS_CODE} on success, otherwise {@link ErrorCode#code()}
 * @param message optional human-readable message, already localised
 * @param data response payload (omitted when null)
 * @param errors error details for failed requests (omitted when empty)
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success, int code, String message, T data, List<ApiError> errors) {

  /** Value of {@link #code()} for successful responses. */
  public static final int SUCCESS_CODE = 1000;

  /** Successful response with a payload. */
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, SUCCESS_CODE, null, data, null);
  }

  /** Successful response with a payload and a message. */
  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, SUCCESS_CODE, message, data, null);
  }

  /** Successful response without a payload. */
  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(true, SUCCESS_CODE, null, null, null);
  }

  /** Failed response for the given error code, with optional field-level details. */
  public static ApiResponse<Void> error(ErrorCode code, String message, List<ApiError> errors) {
    List<ApiError> details = errors == null || errors.isEmpty() ? null : errors;
    return new ApiResponse<>(false, code.code(), message, null, details);
  }

  /** Failed response without field-level details. */
  public static ApiResponse<Void> error(ErrorCode code, String message) {
    return error(code, message, null);
  }
}
