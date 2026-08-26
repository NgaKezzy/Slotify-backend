package com.slotify.common.exception;

import lombok.Getter;

/**
 * Base runtime exception for expected business errors.
 *
 * <p>Throw this (or a subclass) from services; {@link GlobalExceptionHandler} converts it to the
 * proper HTTP status and a localised {@code ApiResponse}. Optional {@code args} are interpolated
 * into the message template.
 */
@Getter
public class AppException extends RuntimeException {

  private final ErrorCode errorCode;
  private final transient Object[] args;

  public AppException(ErrorCode errorCode, Object... args) {
    super(errorCode.name());
    this.errorCode = errorCode;
    this.args = args;
  }
}
