package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared, printer-friendly RentEase PDF layout and typography. */
final class ReportPdfSupport {
    static final Color CHARCOAL = new Color(38, 42, 40);
    static final Color GOLD = new Color(173, 143, 82);
    static final Color PAPER = new Color(248, 248, 246);
    static final Color ALT_ROW = new Color(243, 244, 241);
    static final Color MUTED = new Color(105, 111, 107);
    static final Color INK = new Color(45, 49, 47);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.ENGLISH);

    private ReportPdfSupport() { }

    static PdfPageEventHelper brandingPageEvent() {
        return new BrandingPageEvent();
    }

    static void addReportIntro(Document document, String title, String description, ReportExportConfig export,
                               YearMonth period, Font titleFont) throws DocumentException {
        // Executive-style report masthead: a compact dark panel with a gold rule,
        // followed by clear report metadata. Keep the PDF printer-friendly below it.
        PdfPTable masthead = new PdfPTable(1);
        masthead.setWidthPercentage(100);
        masthead.setSpacingBefore(2);
        masthead.setSpacingAfter(11);
        PdfPCell mastheadCell = new PdfPCell();
        mastheadCell.setBackgroundColor(CHARCOAL);
        mastheadCell.setBorder(Rectangle.LEFT);
        mastheadCell.setBorderColor(GOLD);
        mastheadCell.setBorderWidth(4f);
        mastheadCell.setPaddingTop(13);
        mastheadCell.setPaddingBottom(13);
        mastheadCell.setPaddingLeft(15);
        mastheadCell.setPaddingRight(13);
        Paragraph brand = new Paragraph("RENTEASE  /  BUSINESS INTELLIGENCE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(220, 198, 145)));
        brand.setSpacingAfter(6);
        mastheadCell.addElement(brand);
        Font heroTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 21, Color.WHITE);
        mastheadCell.addElement(new Paragraph(title, heroTitleFont));
        Paragraph subtitle = new Paragraph(description,
                FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(224, 226, 221)));
        subtitle.setSpacingBefore(5);
        mastheadCell.addElement(subtitle);
        masthead.addCell(mastheadCell);
        document.add(masthead);

        PdfPTable metadata = new PdfPTable(3);
        metadata.setWidthPercentage(100);
        metadata.setWidths(new float[] {1.2f, 1.15f, 1.25f});
        metadata.setSpacingAfter(14);
        LocalDateTime generatedAt = export.getGeneratedAt();
        addMetadataCell(metadata, "REPORTING PERIOD", period.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                + " " + period.getYear());
        addMetadataCell(metadata, "GENERATED ON", generatedAt == null ? "—" : DATE_TIME.format(generatedAt));
        addMetadataCell(metadata, "PREPARED BY · STAFF ID", safe(export.getGeneratedByStaffId()));
        document.add(metadata);
    }

    static void addHeading(Document document, String label, Font font) throws DocumentException {
        PdfPTable section = new PdfPTable(1);
        section.setWidthPercentage(100);
        section.setSpacingBefore(4);
        section.setSpacingAfter(6);
        PdfPCell cell = new PdfPCell(new Phrase(label.toUpperCase(Locale.ENGLISH),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, CHARCOAL)));
        cell.setBackgroundColor(new Color(246, 245, 240));
        cell.setBorder(Rectangle.LEFT | Rectangle.BOTTOM);
        cell.setBorderColor(GOLD);
        cell.setBorderWidth(0.8f);
        cell.setPaddingTop(7);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(9);
        cell.setPaddingRight(7);
        section.addCell(cell);
        document.add(section);
    }

    static void addSummaryMetrics(Document document, String[][] metrics) throws DocumentException {
        if (metrics == null || metrics.length == 0) return;
        PdfPTable cards = new PdfPTable(Math.min(4, metrics.length));
        cards.setWidthPercentage(100);
        cards.setSpacingAfter(14);
        for (String[] metric : metrics) {
            String label = metric.length > 0 ? metric[0] : "";
            String value = metric.length > 1 ? metric[1] : "—";
            PdfPCell card = new PdfPCell();
            card.setBackgroundColor(PAPER);
            card.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.RIGHT | Rectangle.LEFT);
            card.setBorderColor(new Color(226, 228, 224));
            card.setBorderWidth(0.6f);
            card.setBorderColorLeft(GOLD);
            card.setBorderWidthLeft(1.8f);
            card.setPaddingTop(10);
            card.setPaddingBottom(10);
            card.setPaddingLeft(10);
            card.setPaddingRight(8);
            Paragraph labelLine = new Paragraph(label.toUpperCase(Locale.ENGLISH),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, MUTED));
            labelLine.setSpacingAfter(5);
            card.addElement(labelLine);
            Paragraph valueLine = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, CHARCOAL));
            card.addElement(valueLine);
            cards.addCell(card);
        }
        document.add(cards);
    }

    static void addEmptyNotice(Document document, String message) throws DocumentException {
        PdfPTable notice = new PdfPTable(1);
        notice.setWidthPercentage(100);
        notice.setSpacingBefore(2);
        notice.setSpacingAfter(10);
        PdfPCell cell = new PdfPCell(new Phrase(message, FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED)));
        cell.setBackgroundColor(PAPER);
        cell.setBorderColor(new Color(226, 228, 224));
        cell.setPadding(9);
        notice.addCell(cell);
        document.add(notice);
    }

    static PdfPTable table(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(12);
        table.setSplitRows(true);
        table.setHeaderRows(1);
        return table;
    }

    static void setWidths(PdfPTable table, float... widths) throws DocumentException {
        table.setWidths(widths);
    }

    static void addHeader(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
            cell.setBackgroundColor(CHARCOAL);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(8);
            cell.setPaddingBottom(8);
            cell.setPaddingLeft(7);
            cell.setPaddingRight(7);
            cell.setNoWrap(false);
            table.addCell(cell);
        }
    }

    static void addRow(PdfPTable table, String... values) {
        addRow(table, false, values);
    }

    static void addHighlightedRow(PdfPTable table, String... values) {
        addRow(table, true, values);
    }

    private static void addRow(PdfPTable table, boolean highlighted, String... values) {
        boolean alternate = table.getRows().size() % 2 == 0;
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value == null ? "—" : value,
                    FontFactory.getFont(FontFactory.HELVETICA, 8, INK)));
            cell.setBackgroundColor(highlighted ? new Color(250, 247, 238) : (alternate ? ALT_ROW : Color.WHITE));
            cell.setBorderColor(new Color(229, 231, 227));
            cell.setBorderWidth(0.35f);
            cell.setPadding(7);
            cell.setUseAscender(true);
            cell.setUseDescender(true);
            if (value != null && value.matches("(?:LKR\\s*)?[\\d,.]+%?")) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }
    }

    static void addKeyValueTable(Document document, String[][] rows, Font bodyFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {1.15f, 2.1f});
        table.setSpacingAfter(12);
        for (String[] row : rows) {
            PdfPCell key = new PdfPCell(new Phrase(row[0], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED)));
            key.setBackgroundColor(PAPER);
            key.setBorderColor(new Color(229, 231, 227));
            key.setPadding(7);
            PdfPCell value = new PdfPCell(new Phrase(row[1], bodyFont));
            value.setBorderColor(new Color(229, 231, 227));
            value.setPadding(7);
            table.addCell(key);
            table.addCell(value);
        }
        document.add(table);
    }

    private static void addMetadataCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColor(new Color(225, 227, 222));
        cell.setBorderWidth(0.6f);
        cell.setPaddingTop(8);
        cell.setPaddingBottom(8);
        cell.setPaddingLeft(7);
        cell.setPaddingRight(7);
        cell.setBackgroundColor(PAPER);
        Paragraph labelLine = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, MUTED));
        labelLine.setSpacingAfter(4);
        cell.addElement(labelLine);
        cell.addElement(new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA, 8, INK)));
        table.addCell(cell);
    }

    static String money(BigDecimal value) {
        return value == null ? "0.00" : String.format(Locale.ENGLISH, "%,.2f", value);
    }

    static String humanDateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    static String safe(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private static final class BrandingPageEvent extends PdfPageEventHelper {
        private PdfTemplate totalPages;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(36, 12);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException ignored) {
                baseFont = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            var canvas = writer.getDirectContent();
            float left = document.left();
            float right = document.right();
            float top = document.top() + 31;
            canvas.saveState();
            canvas.setColorStroke(GOLD);
            canvas.setLineWidth(1.1f);
            canvas.moveTo(left, top - 21);
            canvas.lineTo(right, top - 21);
            canvas.stroke();
            if (baseFont != null) {
                canvas.beginText();
                canvas.setColorFill(CHARCOAL);
                canvas.setFontAndSize(baseFont, 11);
                canvas.showTextAligned(Element.ALIGN_LEFT, "RENTEASE", left, top, 0);
                canvas.setColorFill(MUTED);
                canvas.setFontAndSize(baseFont, 7.5f);
                canvas.showTextAligned(Element.ALIGN_RIGHT, "Vehicle Rental Management System", right, top + 1, 0);
                canvas.endText();
            }

            float footerY = document.bottom() - 25;
            canvas.setColorStroke(new Color(220, 222, 218));
            canvas.setLineWidth(0.6f);
            canvas.moveTo(left, footerY + 16);
            canvas.lineTo(right, footerY + 16);
            canvas.stroke();
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, MUTED);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("RentEase Vehicle Rental Management System  ·  Generated from current system records",
                            footerFont), left, footerY, 0);
            if (totalPages != null) {
                float templateX = right - totalPages.getWidth();
                ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                        new Phrase("Page " + writer.getPageNumber() + " of", footerFont), templateX - 5, footerY, 0);
                canvas.addTemplate(totalPages, templateX, footerY);
            }
            canvas.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPages != null) {
                if (baseFont == null) return;
                totalPages.beginText();
                totalPages.setFontAndSize(baseFont, 7);
                totalPages.setTextMatrix(0, 0);
                totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
                totalPages.endText();
            }
        }
    }
}
