package com.rentease.renteasesystem.reports.repository;

import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MEMBER 6 - Kaviyashini V (IT25101787)
 * (Supporting repository for ReportExportConfig.)
 */
public interface ReportExportConfigRepository extends JpaRepository<ReportExportConfig, Long> {
    List<ReportExportConfig> findByReportType(ReportExportConfig.ReportType reportType);
}
