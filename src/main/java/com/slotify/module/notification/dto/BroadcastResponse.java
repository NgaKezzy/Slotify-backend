package com.slotify.module.notification.dto;

/**
 * Result of a broadcast.
 *
 * @param recipients number of users that received the in-app notification (and a push when they
 *     have a registered device and Firebase is configured)
 */
public record BroadcastResponse(int recipients) {}
