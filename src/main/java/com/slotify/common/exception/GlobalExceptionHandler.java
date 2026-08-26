package com.slotify.common.exception;

import com.slotify.common.api.ApiError;
import com.slotify.common.api.ApiResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into the standard {@link ApiResponse} error envelope.
 *
 * <p>Business errors ({@link AppException}) map to their {@link ErrorCode}; framework exceptions
 * are mapped to the closest generic code. Messages are resolved through {@link MessageSource} using
 * the request locale ({@code Accept-Language} header).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
    ErrorCode code = ex.getErrorCode();
    return build(code, resolve(code, ex.getArgs()), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    List<ApiError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new ApiError(
                        fieldError.getField(),
                        ErrorCode.VALIDATION_FAILED.name(),
                        fieldError.getDefaultMessage()))
            .toList();
    return build(ErrorCode.VALIDATION_FAILED, resolve(ErrorCode.VALIDATION_FAILED), errors);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
    return simple(ErrorCode.BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
    return simple(ErrorCode.NOT_FOUND);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
    return simple(ErrorCode.UNAUTHORIZED);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    return simple(ErrorCode.FORBIDDEN);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return simple(ErrorCode.INTERNAL_ERROR);
  }

  private ResponseEntity<ApiResponse<Void>> simple(ErrorCode code) {
    return build(code, resolve(code), null);
  }

  private ResponseEntity<ApiResponse<Void>> build(
      ErrorCode code, String message, List<ApiError> errors) {
    return ResponseEntity.status(code.status()).body(ApiResponse.error(code, message, errors));
  }

  private String resolve(ErrorCode code, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(code.messageKey(), args, code.name(), locale);
  }
}
