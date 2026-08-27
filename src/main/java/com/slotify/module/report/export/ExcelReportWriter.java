package com.slotify.module.report.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Renders a {@link ReportTable} as a single-sheet {@code .xlsx} workbook (Apache POI). */
@Component
public class ExcelReportWriter {

  private static final int TITLE_FONT_POINTS = 14;
  private static final int MAX_SHEET_NAME = 31;

  /** Writes the table to workbook bytes. */
  public byte[] write(ReportTable table) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(sheetName(table.title()));
      int rowIndex = 0;

      Row titleRow = sheet.createRow(rowIndex++);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue(table.title());
      titleCell.setCellStyle(titleStyle(workbook));
      for (String subtitle : table.subtitles()) {
        sheet.createRow(rowIndex++).createCell(0).setCellValue(subtitle);
      }
      rowIndex++;

      CellStyle headerStyle = headerStyle(workbook);
      Row headerRow = sheet.createRow(rowIndex++);
      for (int col = 0; col < table.headers().size(); col++) {
        Cell cell = headerRow.createCell(col);
        cell.setCellValue(table.headers().get(col));
        cell.setCellStyle(headerStyle);
      }
      for (List<String> values : table.rows()) {
        Row row = sheet.createRow(rowIndex++);
        for (int col = 0; col < values.size(); col++) {
          row.createCell(col).setCellValue(values.get(col));
        }
      }
      for (int col = 0; col < table.headers().size(); col++) {
        sheet.autoSizeColumn(col);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write Excel report", e);
    }
  }

  private static String sheetName(String title) {
    String safe = title.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
    return safe.length() > MAX_SHEET_NAME ? safe.substring(0, MAX_SHEET_NAME) : safe;
  }

  private static CellStyle titleStyle(Workbook workbook) {
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) TITLE_FONT_POINTS);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    return style;
  }

  private static CellStyle headerStyle(Workbook workbook) {
    Font font = workbook.createFont();
    font.setBold(true);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    return style;
  }
}
