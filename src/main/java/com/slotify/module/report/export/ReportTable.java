package com.slotify.module.report.export;

import java.util.List;

/**
 * Format-independent description of an exported report: a title, a few subtitle lines (period,
 * generated-at) and one table.
 *
 * @param title report title, also used as sheet name
 * @param subtitles lines printed under the title
 * @param headers column headers
 * @param rows cell values, one list per row, already formatted as text
 */
public record ReportTable(
    String title, List<String> subtitles, List<String> headers, List<List<String>> rows) {}
