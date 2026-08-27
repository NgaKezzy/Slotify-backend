package com.slotify.module.report.export;

/**
 * A generated download.
 *
 * @param filename suggested file name including extension
 * @param contentType MIME type
 * @param content file bytes
 */
public record ExportedFile(String filename, String contentType, byte[] content) {}
