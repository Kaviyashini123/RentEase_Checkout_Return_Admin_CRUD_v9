package com.rentease.renteasesystem.reports.service;

import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Selects the registered report strategy for the existing report type enum. */
@Component
public class ReportGenerationStrategyRegistry {
    private final Map<ReportExportConfig.ReportType, ReportGenerationStrategy> strategies;

    public ReportGenerationStrategyRegistry(List<ReportGenerationStrategy> strategies) {
        Map<ReportExportConfig.ReportType, ReportGenerationStrategy> registered =
                new EnumMap<>(ReportExportConfig.ReportType.class);
        for (ReportGenerationStrategy strategy : strategies) {
            ReportGenerationStrategy previous = registered.put(strategy.reportType(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Multiple report strategies registered for " + strategy.reportType());
            }
        }
        for (ReportExportConfig.ReportType type : ReportExportConfig.ReportType.values()) {
            if (!registered.containsKey(type)) {
                throw new IllegalStateException("No report-generation strategy registered for " + type);
            }
        }
        this.strategies = Map.copyOf(registered);
    }

    public ReportGenerationStrategy forType(ReportExportConfig.ReportType type) {
        ReportGenerationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported report type: " + type);
        }
        return strategy;
    }
}
