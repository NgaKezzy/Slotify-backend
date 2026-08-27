package com.slotify.module.customer.dto;

/**
 * A customer tag of a salon.
 *
 * @param id tag id
 * @param name label
 * @param color hex colour
 */
public record CustomerTagResponse(Long id, String name, String color) {}
