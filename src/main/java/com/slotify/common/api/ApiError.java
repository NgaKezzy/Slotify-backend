package com.slotify.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Field-level error entry inside {@link ApiResponse#errors()}.
 *
 * <p>Used mainly for validation failures where several request fields can be wrong at once; the
 * overall error is already described by {@link ApiResponse#code()} and {@link
 * ApiResponse#message()}.
 *
 * @param field name of the offending request field (null for non-field errors)
 * @param code machine-readable error code
 * @param message human-readable description
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String field, String code, String message) {}
