package com.slotify.module.salon.dto;

/**
 * Result of {@code POST /admin/uploads/presign}: the client PUTs the file to {@code uploadUrl} and
 * then stores {@code publicUrl} in the entity (salon cover, gallery, service image, avatar).
 *
 * @param uploadUrl pre-signed S3/MinIO PUT URL, valid for a few minutes
 * @param publicUrl URL under which the object will be readable
 * @param objectKey key of the object inside the bucket
 */
public record ImageUploadResponse(String uploadUrl, String publicUrl, String objectKey) {}
