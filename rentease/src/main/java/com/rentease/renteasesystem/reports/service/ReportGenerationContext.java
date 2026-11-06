package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Font;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import java.time.YearMonth;

/** Data and shared PDF helpers supplied to one report-generation strategy. */
public record ReportGenerationContext(
        ReportReviewService reportData,
        ReportExportConfig export,
        YearMonth period,
        Font headingFont,
        Font bodyFont) {

    public void addHeading(com.lowagie.text.Document document, String label)
            throws com.lowagie.text.DocumentException {
        ReportPdfSupport.addHeading(document, label, headingFont);
    }

    public void addSummaryMetrics(Document document, String[][] metrics) throws DocumentException {
        ReportPdfSupport.addSummaryMetrics(document, metrics);
    }

    public void addEmptyNotice(Document document, String message) throws DocumentException {
        ReportPdfSupport.addEmptyNotice(document, message);
    }

    public void setWidths(PdfPTable table, float... widths) throws DocumentException {
        ReportPdfSupport.setWidths(table, widths);
    }

    public void addHighlightedRow(PdfPTable table, String... values) {
        ReportPdfSupport.addHighlightedRow(table, values);
    }

    public String humanDateTime(java.time.LocalDateTime value) {
        return ReportPdfSupport.humanDateTime(value);
    }

    public PdfPTable table(int columns) {
        return ReportPdfSupport.table(columns);
    }

    public void addHeader(PdfPTable table, String... values) {
        ReportPdfSupport.addHeader(table, values);
    }

    public void addRow(PdfPTable table, String... values) {
        ReportPdfSupport.addRow(table, values);
    }

    public void addKeyValueTable(com.lowagie.text.Document document, String[][] rows)
            throws com.lowagie.text.DocumentException {
        ReportPdfSupport.addKeyValueTable(document, rows, bodyFont);
    }

    public String money(java.math.BigDecimal value) {
        return ReportPdfSupport.money(value);
    }

    public String safe(Object value) {
        return ReportPdfSupport.safe(value);
    }
}
