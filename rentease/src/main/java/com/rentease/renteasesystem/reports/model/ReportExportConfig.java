package com.rentease.renteasesystem.reports.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MEMBER 6 - Kaviyashini V (IT25101787)
 * Reports & Reviews Management Module
 * Stores metadata for generated report exports (PDF/CSV).
 */
@Entity
@Table(name = "report_export_configs")
public class ReportExportConfig {

    public enum ExportFormat { PDF, CSV }
    public enum ReportType { MONTHLY_REVENUE, VEHICLE_UTILIZATION, MOST_RENTED_VEHICLES, RATING_ANALYSIS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    private ExportFormat format;

    private String periodLabel; // e.g. "2026-08"

    private String generatedFileName;

    private LocalDateTime generatedAt = LocalDateTime.now();

    private String generatedByStaffId;

    public ReportExportConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }
    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
    public String getGeneratedFileName() { return generatedFileName; }
    public void setGeneratedFileName(String generatedFileName) { this.generatedFileName = generatedFileName; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getGeneratedByStaffId() { return generatedByStaffId; }
    public void setGeneratedByStaffId(String generatedByStaffId) { this.generatedByStaffId = generatedByStaffId; }
}
