package com.slotify.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Injects the signed-in {@link UserPrincipal} into a controller method parameter.
 *
 * <pre>{@code
 * @GetMapping("/me")
 * public ApiResponse<UserResponse> me(@CurrentUser UserPrincipal principal) { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {}
