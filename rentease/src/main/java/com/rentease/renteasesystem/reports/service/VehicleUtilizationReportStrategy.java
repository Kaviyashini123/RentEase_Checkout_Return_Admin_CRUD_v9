package com.rentease.renteasesystem.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates fleet utilization using the same utilization KPI calculation as the overview.
 * Empty catalogues still produce a valid summary and a header-only catalogue table.
 */
@Component
public class VehicleUtilizationReportStrategy implements ReportGenerationStrategy {
    private static final Logger log = LoggerFactory.getLogger(VehicleUtilizationReportStrategy.class);
    @Override public ReportExportConfig.ReportType reportType() { return ReportExportConfig.ReportType.VEHICLE_UTILIZATION; }
    @Override public String title() { return "Vehicle Utilization Report"; }
    @Override public String description() { return "Current fleet snapshot using vehicles covered by pending and confirmed reservations; this is not a historical month-end snapshot."; }

    @Override
    public void generate(Document document, ReportGenerationContext context) throws DocumentException {
        List<Vehicle> loadedVehicles;
        List<Reservation> loadedReservations;
        try {
            loadedVehicles = context.reportData().getOverviewVehicles();
            loadedReservations = context.reportData().getOverviewReservations();
        } catch (RuntimeException exception) {
            log.error("PDF generation failed: reportType={}, operation=load-fleet-and-reservations",
                    reportType(), exception);
            throw exception;
        }
        List<Vehicle> vehicles = loadedVehicles == null ? List.of()
                : loadedVehicles.stream().filter(Objects::nonNull).toList();
        List<Reservation> reservations = loadedReservations == null ? List.of()
                : loadedReservations.stream().filter(Objects::nonNull).toList();
        double utilization;
        long reservedVehicles;
        long pendingReservations;
        long confirmedReservations;
        try {
            utilization = context.reportData().getVehicleUtilizationRate(vehicles, reservations);
            Set<Long> fleetVehicleIds = vehicles.stream().map(Vehicle::getId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            reservedVehicles = reservations.stream()
                    .filter(reservation -> reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED
                            || reservation.getStatus() == Reservation.ReservationStatus.PENDING)
                    .map(Reservation::getVehicleId)
                    .filter(fleetVehicleIds::contains)
                    .distinct()
                    .count();
            pendingReservations = reservations.stream()
                    .filter(reservation -> reservation.getStatus() == Reservation.ReservationStatus.PENDING).count();
            confirmedReservations = reservations.stream()
                    .filter(reservation -> reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED).count();
        } catch (RuntimeException exception) {
            log.error("PDF generation failed: reportType={}, operation=calculate-utilization-and-status-counts",
                    reportType(), exception);
            throw exception;
        }

        try {
            writeReportContent(document, context, vehicles, utilization, reservedVehicles,
                    pendingReservations, confirmedReservations);
        } catch (RuntimeException exception) {
            log.error("PDF generation failed: reportType={}, operation=render-utilization-pdf-content",
                    reportType(), exception);
            throw exception;
        }
    }

    private void writeReportContent(Document document, ReportGenerationContext context,
                                    List<Vehicle> vehicles, double utilization, long reservedVehicles,
                                    long pendingReservations, long confirmedReservations) throws DocumentException {
        context.addSummaryMetrics(document, new String[][] {
                {"Total fleet", String.valueOf(vehicles.size())},
                {"Utilization rate", String.format(java.util.Locale.ENGLISH, "%.1f%%", utilization)},
                {"Vehicles in reservations", String.valueOf(reservedVehicles)},
                {"Pending reservations", String.valueOf(pendingReservations)}
        });
        context.addHeading(document, "Utilization Summary");
        context.addKeyValueTable(document, new String[][] {
                {"Total fleet", String.valueOf(vehicles.size())},
                {"Available vehicles", String.valueOf(count(vehicles, Vehicle.VehicleStatus.AVAILABLE))},
                {"Booked vehicles", String.valueOf(count(vehicles, Vehicle.VehicleStatus.BOOKED))},
                {"Under-maintenance vehicles", String.valueOf(count(vehicles, Vehicle.VehicleStatus.UNDER_MAINTENANCE))},
                {"Inactive vehicles", String.valueOf(count(vehicles, Vehicle.VehicleStatus.INACTIVE))},
                {"Pending reservations", String.valueOf(pendingReservations)},
                {"Confirmed reservations", String.valueOf(confirmedReservations)},
                {"Vehicles in active/pending reservations", String.valueOf(reservedVehicles)},
                {"Utilization rate", String.format(java.util.Locale.ENGLISH, "%.1f%%", utilization)}
        });
        context.addHeading(document, "Vehicle Catalogue");
        if (vehicles.isEmpty()) context.addEmptyNotice(document, "No vehicles are available in the fleet catalogue for this snapshot.");
        PdfPTable table = context.table(5);
        context.setWidths(table, 0.55f, 1.75f, 1.0f, 1.35f, 1.0f);
        context.addHeader(table, "ID", "Vehicle", "Type", "Location", "Status");
        vehicles.forEach(vehicle -> context.addRow(table, String.valueOf(vehicle.getId()),
                context.safe(vehicle.getBrand()) + " " + context.safe(vehicle.getModel()),
                context.safe(vehicle.getType()), context.safe(vehicle.getLocation()), context.safe(vehicle.getStatus())));
        document.add(table);
    }

    private long count(List<Vehicle> vehicles, Vehicle.VehicleStatus status) {
        return vehicles.stream().filter(vehicle -> vehicle.getStatus() == status).count();
    }
}
