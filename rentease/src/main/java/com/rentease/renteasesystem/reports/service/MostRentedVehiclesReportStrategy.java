package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Generates the reservation ranking from the existing most-rented calculation. */
@Component
public class MostRentedVehiclesReportStrategy implements ReportGenerationStrategy {
    @Override public ReportExportConfig.ReportType reportType() { return ReportExportConfig.ReportType.MOST_RENTED_VEHICLES; }
    @Override public String title() { return "Most-Rented Vehicles Report"; }
    @Override public String description() { return "Top five vehicles ranked by all recorded reservation history; the selected month is retained as export metadata."; }

    @Override
    public void generate(Document document, ReportGenerationContext context) throws DocumentException {
        var vehicles = context.reportData().getOverviewVehicles();
        Map<Long, Vehicle> vehiclesById = vehicles.stream()
                .filter(vehicle -> vehicle.getId() != null)
                .collect(Collectors.toMap(Vehicle::getId, Function.identity(), (first, ignored) -> first));
        Map<Long, Long> topVehicles = context.reportData().getMostRentedVehicles(5);
        Map.Entry<Long, Long> leader = topVehicles.entrySet().stream().findFirst().orElse(null);
        Vehicle leadingVehicle = leader == null ? null : vehiclesById.get(leader.getKey());
        String leaderName = leader == null ? "No ranked vehicles"
                : leadingVehicle == null ? "Unknown vehicle"
                : (context.safe(leadingVehicle.getBrand()) + " " + context.safe(leadingVehicle.getModel())).trim();
        context.addSummaryMetrics(document, new String[][] {
                {"Total vehicles", String.valueOf(vehicles.size())},
                {"Vehicles ranked", String.valueOf(topVehicles.size())},
                {"Top vehicle", leaderName},
                {"Highest reservations", leader == null ? "—" : String.valueOf(leader.getValue())}
        });
        context.addHeading(document, "Rental Ranking");
        if (topVehicles.isEmpty()) context.addEmptyNotice(document, "No reservation records are available for vehicle ranking.");
        PdfPTable table = context.table(4);
        context.setWidths(table, 0.48f, 0.72f, 2.9f, 1.0f);
        context.addHeader(table, "Rank", "Vehicle ID", "Vehicle", "Reservations");
        int[] rank = {1};
        topVehicles.forEach((vehicleId, count) -> {
            Vehicle vehicle = vehiclesById.get(vehicleId);
            String name = vehicle == null ? "Unknown vehicle"
                    : context.safe(vehicle.getBrand()) + " " + context.safe(vehicle.getModel());
            String[] row = {String.valueOf(rank[0]), String.valueOf(vehicleId), name, String.valueOf(count)};
            if (rank[0] == 1) context.addHighlightedRow(table, row);
            else context.addRow(table, row);
            rank[0]++;
        });
        document.add(table);
    }
}
