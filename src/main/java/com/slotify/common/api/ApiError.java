package com.slotify.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Single error entry inside {@link ApiResponse#errors()}.
 *
 * @param code machine-readable error code (see {@link com.slotify.common.exception.ErrorCode})
 * @param message human-readable description
 * @param field name of the offending request field, when the error is a validation error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, String field) {

  /** Creates a non-field error. */
  public static ApiError of(String code, String message) {
    return new ApiError(code, message, null);
  }
}
