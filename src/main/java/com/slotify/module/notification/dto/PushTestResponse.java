package com.slotify.module.notification.dto;

/**
 * Result of {@code POST /me/push-test}.
 *
 * @param enabled whether the server has Firebase credentials and can send pushes at all
 * @param devices number of device tokens registered for the caller
 * @param delivered number of devices Firebase accepted the message for
 */
public record PushTestResponse(boolean enabled, int devices, int delivered) {}
