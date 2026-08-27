package com.slotify.module.report.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/** Renders a {@link ReportTable} as a landscape A4 PDF (OpenPDF). */
@Component
public class PdfReportWriter {

  private static final float TITLE_SIZE = 16f;
  private static final float BODY_SIZE = 9f;
  private static final float SPACING = 12f;
  private static final float CELL_PADDING = 4f;
  private static final Color HEADER_BACKGROUND = new Color(230, 230, 230);

  /** Writes the table to PDF bytes. */
  public byte[] write(ReportTable table) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4.rotate());
    PdfWriter.getInstance(document, out);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, TITLE_SIZE);
    Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, BODY_SIZE);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, BODY_SIZE);

    document.add(new Paragraph(table.title(), titleFont));
    for (String subtitle : table.subtitles()) {
      document.add(new Paragraph(subtitle, bodyFont));
    }
    Paragraph gap = new Paragraph(" ");
    gap.setSpacingAfter(SPACING);
    document.add(gap);

    PdfPTable pdfTable = new PdfPTable(table.headers().size());
    pdfTable.setWidthPercentage(100);
    for (String header : table.headers()) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setBackgroundColor(HEADER_BACKGROUND);
      cell.setPadding(CELL_PADDING);
      pdfTable.addCell(cell);
    }
    pdfTable.setHeaderRows(1);
    for (List<String> row : table.rows()) {
      for (String value : row) {
        PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
        cell.setPadding(CELL_PADDING);
        pdfTable.addCell(cell);
      }
    }
    document.add(pdfTable);
    document.close();
    return out.toByteArray();
  }
}
