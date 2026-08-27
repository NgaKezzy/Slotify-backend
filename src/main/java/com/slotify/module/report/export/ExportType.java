package com.slotify.module.report.export;

/** File format of a report export ({@code type} query parameter, matched case-insensitively). */
public enum ExportType {
  EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
  PDF("pdf", "application/pdf");

  private final String extension;
  private final String contentType;

  ExportType(String extension, String contentType) {
    this.extension = extension;
    this.contentType = contentType;
  }

  public String extension() {
    return extension;
  }

  public String contentType() {
    return contentType;
  }
}
