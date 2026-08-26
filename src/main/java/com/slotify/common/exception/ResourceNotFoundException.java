package com.slotify.common.exception;

/** Thrown when an entity referenced by the request does not exist (HTTP 404). */
public class ResourceNotFoundException extends AppException {

  /**
   * @param resource entity name shown to the user, e.g. {@code "Salon"}
   * @param id identifier that was looked up
   */
  public ResourceNotFoundException(String resource, Object id) {
    super(ErrorCode.NOT_FOUND, resource, id);
  }
}
