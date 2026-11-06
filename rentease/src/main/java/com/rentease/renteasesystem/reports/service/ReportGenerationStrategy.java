package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;

/**
 * Strategy contract for writing one report type into the shared OpenPDF document.
 * Report-specific content belongs to the strategy; export validation, metadata,
 * common document framing, and download behavior remain in ReportReviewService.
 */
public interface ReportGenerationStrategy {
    ReportExportConfig.ReportType reportType();

    String title();

    String description();

    void generate(Document document, ReportGenerationContext context) throws DocumentException;
}
