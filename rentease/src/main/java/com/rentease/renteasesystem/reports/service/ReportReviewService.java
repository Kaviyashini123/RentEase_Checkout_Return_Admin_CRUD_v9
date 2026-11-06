package com.rentease.renteasesystem.reports.service;

import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.reservation.repository.ReservationRepository;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.booking.repository.VehicleRepository;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.payment.repository.PaymentRepository;
import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import com.rentease.renteasesystem.checkout.repository.CheckoutRepository;
import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import com.rentease.renteasesystem.reports.model.Review;
import com.rentease.renteasesystem.reports.repository.ReportExportConfigRepository;
import com.rentease.renteasesystem.reports.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;

/**
 * MEMBER 6 - Kaviyashini V (IT25101787)
 * Reports & Reviews Management Module - Business Logic
 * Aggregates monthly revenue, vehicle utilization, most-rented vehicles, and
 * rating analysis for the Business Development Manager dashboard. Also
 * simulates PDF/CSV export formatting and handles customer review submissions.
 *
 * NOTE: This service reads from other modules' repositories (Payment, Reservation and Booking) purely for aggregation/reporting purposes - the standard pattern for
 * a cross-cutting analytics/reporting layer in a modular monolith. Each
 * member's own package still exclusively OWNS and writes to its own tables.
 */
@Service
public class ReportReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReportReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ReportExportConfigRepository reportExportConfigRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final CheckoutRepository checkoutRepository;
    private final ReportGenerationStrategyRegistry reportStrategyRegistry;

    public ReportReviewService(ReviewRepository reviewRepository,
                                ReportExportConfigRepository reportExportConfigRepository,
                                PaymentRepository paymentRepository,
                                ReservationRepository reservationRepository,
                                VehicleRepository vehicleRepository,
                                CheckoutRepository checkoutRepository,
                                ReportGenerationStrategyRegistry reportStrategyRegistry) {
        this.reviewRepository = reviewRepository;
        this.reportExportConfigRepository = reportExportConfigRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.checkoutRepository = checkoutRepository;
        this.reportStrategyRegistry = reportStrategyRegistry;
    }

    // ---------- Revenue ----------

    public BigDecimal getMonthlyRevenue(YearMonth month) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                .filter(p -> p.getPaidAt() != null && YearMonth.from(p.getPaidAt()).equals(month))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Real successful-payment revenue for each month in the requested window. */
    public List<Map<String, Object>> getRevenueTimeline(YearMonth endingMonth, int monthCount) {
        Map<YearMonth, BigDecimal> monthlyTotals = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == Payment.PaymentStatus.SUCCESS)
                .filter(payment -> payment.getPaidAt() != null && payment.getAmount() != null)
                .collect(Collectors.groupingBy(payment -> YearMonth.from(payment.getPaidAt()),
                        Collectors.mapping(Payment::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = Math.max(monthCount, 1) - 1; offset >= 0; offset--) {
            YearMonth month = endingMonth.minusMonths(offset);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month.toString());
            point.put("label", month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            point.put("amount", monthlyTotals.getOrDefault(month, BigDecimal.ZERO));
            series.add(point);
        }
        return series;
    }

    public List<Vehicle> getOverviewVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Reservation> getOverviewReservations() {
        return reservationRepository.findAll();
    }

    public List<Payment> getOverviewPayments() {
        return paymentRepository.findAll();
    }

    public List<CheckoutRecord> getOverviewCheckoutRecords() {
        return checkoutRepository.findAll();
    }

    public Map<String, Long> getFleetStatusCounts(List<Vehicle> vehicles) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Vehicle.VehicleStatus status : Vehicle.VehicleStatus.values()) {
            counts.put(status.name(), vehicles.stream().filter(vehicle -> vehicle.getStatus() == status).count());
        }
        return counts;
    }

    public Map<String, Long> getReservationStatusCounts(List<Reservation> reservations) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Reservation.ReservationStatus status : Reservation.ReservationStatus.values()) {
            counts.put(status.name(), reservations.stream().filter(reservation -> reservation.getStatus() == status).count());
        }
        return counts;
    }

    public Map<String, Object> getPaymentStatusSummary(List<Payment> payments) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Payment.PaymentStatus status : Payment.PaymentStatus.values()) {
            List<Payment> matching = payments.stream().filter(payment -> payment.getStatus() == status).toList();
            BigDecimal amount = matching.stream().map(Payment::getAmount).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", status.name());
            row.put("label", status.name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
            row.put("count", matching.size());
            row.put("amount", amount);
            summary.put(status.name(), row);
        }
        return summary;
    }

    /** Reservation counts grouped by their real creation month and enum status. */
    public List<Map<String, Object>> getReservationTimeline(YearMonth endingMonth, int monthCount,
                                                            List<Reservation> reservations) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = Math.max(monthCount, 1) - 1; offset >= 0; offset--) {
            YearMonth month = endingMonth.minusMonths(offset);
            Map<String, Long> counts = new LinkedHashMap<>();
            long total = 0;
            for (Reservation.ReservationStatus status : Reservation.ReservationStatus.values()) {
                long count = reservations.stream()
                        .filter(reservation -> reservation.getStatus() == status && reservation.getCreatedAt() != null
                                && YearMonth.from(reservation.getCreatedAt()).equals(month))
                        .count();
                counts.put(status.name(), count);
                total += count;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month.toString());
            point.put("label", month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            point.put("counts", counts);
            point.put("total", total);
            series.add(point);
        }
        return series;
    }

    /** Completed rentals only, joined to their real vehicle record and image URL. */
    public List<Map<String, Object>> getTopCompletedVehicles(int topN, List<Reservation> reservations,
                                                               List<Vehicle> vehicles) {
        Map<Long, Vehicle> byId = vehicles.stream().collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle,
                (first, ignored) -> first));
        Map<Long, Long> counts = reservations.stream()
                .filter(reservation -> reservation.getStatus() == Reservation.ReservationStatus.COMPLETED)
                .collect(Collectors.groupingBy(Reservation::getVehicleId, Collectors.counting()));
        long maximum = counts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        return counts.entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(Math.max(topN, 0)).map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("vehicle", byId.get(entry.getKey()));
                    row.put("vehicleId", entry.getKey());
                    row.put("count", entry.getValue());
                    row.put("percent", maximum == 0 ? 0 : Math.round(entry.getValue() * 100.0 / maximum));
                    return row;
                }).toList();
    }

    /** Events use timestamps already stored by the reservation, payment, checkout and review modules. */
    public List<Map<String, Object>> getRecentOperationsActivity(int limit, List<Reservation> reservations,
                                                                   List<Payment> payments,
                                                                   List<CheckoutRecord> checkouts,
                                                                   List<Review> reviews,
                                                                   List<Vehicle> vehicles) {
        Map<Long, Vehicle> byId = vehicles.stream().collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle,
                (first, ignored) -> first));
        List<Map<String, Object>> events = new ArrayList<>();
        for (Reservation reservation : reservations) {
            LocalDateTime timestamp = reservation.getUpdatedAt() != null ? reservation.getUpdatedAt() : reservation.getCreatedAt();
            if (timestamp == null) continue;
            String status = reservation.getStatus() == null ? "Reservation" : reservation.getStatus().name();
            events.add(activity("Reservation", vehicleName(byId.get(reservation.getVehicleId()), reservation.getVehicleId()),
                    status + " · Reservation #" + reservation.getId(), timestamp, "reservation"));
        }
        for (Payment payment : payments) {
            if (payment.getStatus() != Payment.PaymentStatus.SUCCESS || payment.getPaidAt() == null) continue;
            events.add(activity("Payment completed", "Reservation #" + payment.getReservationId(),
                    "LKR " + (payment.getAmount() == null ? "0.00" : payment.getAmount().toPlainString()),
                    payment.getPaidAt(), "payment"));
        }
        for (CheckoutRecord checkout : checkouts) {
            Vehicle vehicle = byId.get(checkout.getVehicleId());
            if (checkout.getHandoverTimestamp() != null) {
                events.add(activity("Vehicle checked out", vehicleName(vehicle, checkout.getVehicleId()),
                        "Reservation #" + checkout.getReservationId(), checkout.getHandoverTimestamp(), "checkout"));
            }
            if (checkout.getStage() == CheckoutRecord.RecordStage.RETURNED && checkout.getReturnTimestamp() != null) {
                events.add(activity("Vehicle returned", vehicleName(vehicle, checkout.getVehicleId()),
                        "Reservation #" + checkout.getReservationId(), checkout.getReturnTimestamp(), "return"));
            }
        }
        for (Review review : reviews) {
            if (review.getSubmittedAt() == null) continue;
            events.add(activity("Review submitted", vehicleName(byId.get(review.getVehicleId()), review.getVehicleId()),
                    review.getRating() + " / 5 rating", review.getSubmittedAt(), "review"));
        }
        return events.stream().sorted(Comparator.comparing((Map<String, Object> event) ->
                        (LocalDateTime) event.get("timestamp"), Comparator.reverseOrder()))
                .limit(Math.max(limit, 0)).toList();
    }

    private Map<String, Object> activity(String title, String subject, String detail,
                                         LocalDateTime timestamp, String kind) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("title", title);
        event.put("subject", subject);
        event.put("detail", detail);
        event.put("timestamp", timestamp);
        event.put("kind", kind);
        return event;
    }

    private String vehicleName(Vehicle vehicle, Long vehicleId) {
        if (vehicle == null) return "Vehicle #" + vehicleId;
        return (vehicle.getBrand() == null ? "" : vehicle.getBrand()) + " "
                + (vehicle.getModel() == null ? "" : vehicle.getModel()).trim();
    }

    // ---------- Vehicle utilization ----------

    /** Percentage of catalog vehicles currently covered by active reservations. */
    public double getVehicleUtilizationRate() {
        return getVehicleUtilizationRate(vehicleRepository.findAll(), reservationRepository.findAll());
    }

    /** Same dashboard calculation applied to an already loaded report snapshot. */
    public double getVehicleUtilizationRate(List<Vehicle> vehicles, List<Reservation> reservations) {
        List<Vehicle> all = vehicles == null ? List.of()
                : vehicles.stream().filter(java.util.Objects::nonNull).toList();
        if (all.isEmpty()) return 0.0;
        Set<Long> fleetVehicleIds = all.stream().map(Vehicle::getId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        long busy = (reservations == null ? List.<Reservation>of() : reservations).stream()
                .filter(java.util.Objects::nonNull)
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMED
                        || r.getStatus() == Reservation.ReservationStatus.PENDING)
                .map(Reservation::getVehicleId)
                .filter(fleetVehicleIds::contains)
                .distinct()
                .count();
        return (busy * 100.0) / all.size();
    }

    // ---------- Most-rented vehicles ----------

    public Map<Long, Long> getMostRentedVehicles(int topN) {
        List<Reservation> reservations = reservationRepository.findAll();
        return reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getVehicleId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }

    // ---------- Rating analysis ----------

    public Double getAverageRatingForVehicle(Long vehicleId) {
        Double avg = reviewRepository.findAverageRatingForVehicle(vehicleId);
        return avg == null ? 0.0 : avg;
    }

    public List<Review> getReviewsForVehicle(Long vehicleId) {
        return reviewRepository.findByVehicleId(vehicleId).stream()
                .sorted(Comparator.comparing(Review::getSubmittedAt).reversed())
                .toList();
    }

    /** Completed customer reservations that have not yet been reviewed. */
    public List<Reservation> getReservationsAwaitingReview(Long customerId) {
        Set<Long> reviewedReservationIds = getReviewedReservationIds(customerId);
        return reservationRepository.findByCustomerId(customerId).stream()
                .filter(reservation -> reservation.getStatus() == Reservation.ReservationStatus.COMPLETED)
                .filter(reservation -> !reviewedReservationIds.contains(reservation.getId()))
                .filter(reservation -> !reviewRepository.existsByReservationId(reservation.getId()))
                .sorted(Comparator.comparing(Reservation::getEndDate).reversed())
                .toList();
    }

    public Set<Long> getReviewedReservationIds(Long customerId) {
        Set<Long> reservationIds = new HashSet<>();
        reviewRepository.findByCustomerId(customerId).stream()
                .map(Review::getReservationId)
                .filter(java.util.Objects::nonNull)
                .forEach(reservationIds::add);
        return reservationIds;
    }

    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
    }

    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
    }

    public boolean hasReviewForReservation(Long reservationId) {
        return reviewRepository.existsByReservationId(reservationId);
    }

    public boolean hasReviewForReservation(Long reservationId, Long customerId) {
        return reviewRepository.findByReservationId(reservationId).stream()
                .anyMatch(review -> customerId.equals(review.getCustomerId()));
    }

    // ---------- Reviews ----------

    @Transactional
    public Review submitReview(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (review.getReservationId() != null) {
            Reservation reservation = getReservation(review.getReservationId());
            if (reservation.getStatus() != Reservation.ReservationStatus.COMPLETED
                    || !reservation.getVehicleId().equals(review.getVehicleId())
                    || !reservation.getCustomerId().equals(review.getCustomerId())) {
                throw new IllegalArgumentException("Review details must match an eligible completed reservation.");
            }
            if (reviewRepository.existsByReservationId(review.getReservationId())) {
                throw new IllegalArgumentException("This reservation already has feedback.");
            }
        }
        // simple profanity/length heuristic auto-flag - real moderation would be more robust
        review.setFlagged(review.getComment() != null && review.getComment().length() > 1500);
        // Internal workflow fields are staff-managed; never accept them from customer review input.
        review.setManagementStatus(Review.ManagementStatus.OPEN);
        review.setManagementNote(null);
        review.setManagementUpdatedAt(null);
        return reviewRepository.save(review);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    /** Real per-vehicle aggregates linked through completed reservation records. */
    public List<Object[]> getCompletedRentalVehicleRatingSummaries() {
        return reviewRepository.findCompletedRentalVehicleRatingSummaries(Reservation.ReservationStatus.COMPLETED);
    }

    /** Vehicles whose completed-rental average is at or below the review attention threshold. */
    public Set<Long> getCompletedRentalVehicleIdsNeedingAttention(double threshold) {
        return new HashSet<>(reviewRepository.findCompletedRentalVehicleIdsNeedingAttention(
                Reservation.ReservationStatus.COMPLETED, threshold));
    }

    // ---------- CRUD: Reviews ----------

    @Transactional
    public Review updateReview(Long id, Review input) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + id));
        if (input.getRating() < 1 || input.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        review.setCustomerId(input.getCustomerId());
        review.setVehicleId(input.getVehicleId());
        review.setReservationId(input.getReservationId());
        review.setRating(input.getRating());
        review.setComment(input.getComment());
        review.setFlagged(input.isFlagged());
        return reviewRepository.save(review);
    }

    /** Updates only internal management fields; customer feedback and attention flags are untouched. */
    @Transactional
    public Review updateReviewManagement(Long id, Review.ManagementStatus status, String note) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + id));
        if (status == null) throw new IllegalArgumentException("A review-management status is required.");
        if (note != null && note.length() > 2000) {
            throw new IllegalArgumentException("The internal note cannot exceed 2,000 characters.");
        }
        String normalizedNote = note == null || note.isBlank() ? null : note.trim();
        review.setManagementStatus(status);
        review.setManagementNote(normalizedNote);
        review.setManagementUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + id));
        reviewRepository.deleteById(id);
    }

    // ---------- CRUD: Report exports ----------

    public ReportExportConfig getExport(Long id) {
        return reportExportConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report export not found with id: " + id));
    }

    @Transactional
    public ReportExportConfig updateExport(Long id, ReportExportConfig input) {
        ReportExportConfig config = getExport(id);
        config.setReportType(input.getReportType());
        config.setFormat(input.getFormat());
        config.setPeriodLabel(input.getPeriodLabel());
        config.setGeneratedFileName(input.getGeneratedFileName());
        config.setGeneratedByStaffId(input.getGeneratedByStaffId());
        return saveReportExport(config, "update-report-export");
    }

    @Transactional
    public void deleteExport(Long id) {
        getExport(id);
        reportExportConfigRepository.deleteById(id);
    }

    // ---------- Real report export ----------

    @Transactional
    public ReportExportConfig exportReport(ReportExportConfig.ReportType type,
                                            ReportExportConfig.ExportFormat format,
                                            String periodLabel,
                                            String staffId) {
        if (periodLabel == null || !periodLabel.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Period must be in YYYY-MM format.");
        }
        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("Staff ID is required.");
        }

        ReportExportConfig config = new ReportExportConfig();
        config.setReportType(type);
        config.setFormat(format);
        config.setPeriodLabel(periodLabel);
        config.setGeneratedByStaffId(staffId.trim());
        config.setGeneratedAt(LocalDateTime.now());
        config.setGeneratedFileName(type.name().toLowerCase() + "_" + periodLabel + "." + format.name().toLowerCase());
        return saveReportExport(config, "create-report-export");
    }

    /** Flush inside this boundary so database constraint failures are logged as persistence errors. */
    private ReportExportConfig saveReportExport(ReportExportConfig config, String operation) {
        try {
            return reportExportConfigRepository.saveAndFlush(config);
        } catch (DataAccessException exception) {
            log.error("Report history persistence failed: operation={}, reportType={}, format={}, exportId={}",
                    operation, config.getReportType(), config.getFormat(), config.getId(), exception);
            throw exception;
        }
    }

    /**
     * Orchestrates OpenPDF generation and delegates report-specific content to the
     * strategy registered for the export's persisted report type.
     */
    public byte[] generatePdf(Long exportId) {
        ReportExportConfig config = getExport(exportId);
        try {
            YearMonth month = YearMonth.parse(config.getPeriodLabel());
            ReportGenerationStrategy strategy = reportStrategyRegistry.forType(config.getReportType());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(
                    config.getReportType() == ReportExportConfig.ReportType.MONTHLY_REVENUE
                            ? com.lowagie.text.PageSize.A4.rotate() : com.lowagie.text.PageSize.A4,
                    42, 42, 76, 62);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(ReportPdfSupport.brandingPageEvent());
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 21, ReportPdfSupport.CHARCOAL);
            Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ReportPdfSupport.CHARCOAL);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            ReportPdfSupport.addReportIntro(document, strategy.title(), strategy.description(), config, month, title);

            try {
                strategy.generate(document, new ReportGenerationContext(this, config, month, heading, body));
            } catch (RuntimeException exception) {
                log.error("PDF generation failed: exportId={}, reportType={}, period={}, strategy={}",
                        config.getId(), config.getReportType(), config.getPeriodLabel(),
                        strategy.getClass().getSimpleName(), exception);
                throw exception;
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Unable to generate PDF report.", e);
        } catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("The saved report period must be in YYYY-MM format.", e);
        }
    }


    /** Generate a spreadsheet-friendly CSV from the same live records used by the PDF strategies. */
    public byte[] generateCsv(Long exportId) {
        ReportExportConfig config = getExport(exportId);
        YearMonth month;
        try {
            month = YearMonth.parse(config.getPeriodLabel());
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("The saved report period must be in YYYY-MM format.", exception);
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"RentEase Business Report", config.getReportType().name().replace('_', ' ')});
        rows.add(new String[] {"Reporting period", month.toString()});
        rows.add(new String[] {"Generated at", config.getGeneratedAt() == null ? "" : config.getGeneratedAt().toString()});
        rows.add(new String[] {"Generated by staff ID", config.getGeneratedByStaffId() == null ? "" : config.getGeneratedByStaffId()});
        rows.add(new String[] {});

        switch (config.getReportType()) {
            case MONTHLY_REVENUE -> {
                List<Payment> all = getOverviewPayments();
                List<Payment> monthly = all.stream().filter(p -> p.getPaidAt() != null
                        && YearMonth.from(p.getPaidAt()).equals(month)).toList();
                BigDecimal revenue = getMonthlyRevenue(month);
                BigDecimal previous = getMonthlyRevenue(month.minusMonths(1));
                BigDecimal change = revenue.subtract(previous);
                rows.add(new String[] {"EXECUTIVE SUMMARY"});
                rows.add(new String[] {"Metric", "Value"});
                rows.add(new String[] {"Successful revenue (LKR)", revenue.toPlainString()});
                rows.add(new String[] {"Previous month revenue (LKR)", previous.toPlainString()});
                rows.add(new String[] {"Revenue movement (LKR)", change.toPlainString()});
                rows.add(new String[] {"Successful payments", String.valueOf(monthly.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS).count())});
                rows.add(new String[] {"Transactions with paid date in month", String.valueOf(monthly.size())});
                rows.add(new String[] {});
                rows.add(new String[] {"PAYMENT METHOD MIX"});
                rows.add(new String[] {"Payment method", "Transaction count", "Recorded amount (LKR)"});
                for (Payment.PaymentMethod method : Payment.PaymentMethod.values()) {
                    List<Payment> matching = monthly.stream().filter(p -> p.getMethod() == method).toList();
                    BigDecimal amount = matching.stream().map(Payment::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    rows.add(new String[] {method.name(), String.valueOf(matching.size()), amount.toPlainString()});
                }
                rows.add(new String[] {});
                rows.add(new String[] {"CURRENT PAYMENT STATUS SNAPSHOT (ALL DATES)"});
                rows.add(new String[] {"Status", "Record count", "Recorded amount (LKR)"});
                for (Payment.PaymentStatus status : Payment.PaymentStatus.values()) {
                    List<Payment> matching = all.stream().filter(p -> p.getStatus() == status).toList();
                    BigDecimal amount = matching.stream().map(Payment::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    rows.add(new String[] {status.name(), String.valueOf(matching.size()), amount.toPlainString()});
                }
                rows.add(new String[] {});
                rows.add(new String[] {"MONTHLY TRANSACTION REGISTER"});
                rows.add(new String[] {"Payment ID", "Reservation ID", "Customer ID", "Amount (LKR)", "Method", "Status", "Paid at"});
                monthly.stream().sorted(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .forEach(p -> rows.add(new String[] {str(p.getId()), str(p.getReservationId()), str(p.getCustomerId()),
                                moneyCsv(p.getAmount()), str(p.getMethod()), str(p.getStatus()), str(p.getPaidAt())}));
                rows.add(new String[] {});
                rows.add(new String[] {"Revenue rule", "Successful payments with a paid timestamp in the selected month."});
                rows.add(new String[] {"Refund rule", "Refunded payments are reported separately; they are not silently netted from revenue."});
            }
            case VEHICLE_UTILIZATION -> {
                List<Vehicle> vehicles = getOverviewVehicles();
                List<Reservation> reservations = getOverviewReservations();
                double utilization = getVehicleUtilizationRate(vehicles, reservations);
                rows.add(new String[] {"FLEET SUMMARY"});
                rows.add(new String[] {"Metric", "Value"});
                rows.add(new String[] {"Total fleet", String.valueOf(vehicles.size())});
                rows.add(new String[] {"Utilization rate (%)", String.format(Locale.ROOT, "%.1f", utilization)});
                for (Vehicle.VehicleStatus status : Vehicle.VehicleStatus.values()) {
                    rows.add(new String[] {"Fleet status: " + status.name(), String.valueOf(vehicles.stream().filter(v -> v.getStatus() == status).count())});
                }
                rows.add(new String[] {});
                rows.add(new String[] {"VEHICLE CATALOGUE"});
                rows.add(new String[] {"Vehicle ID", "Brand", "Model", "Type", "Location", "Status"});
                vehicles.forEach(v -> rows.add(new String[] {str(v.getId()), str(v.getBrand()), str(v.getModel()), str(v.getType()), str(v.getLocation()), str(v.getStatus())}));
            }
            case MOST_RENTED_VEHICLES -> {
                List<Vehicle> vehicles = getOverviewVehicles();
                Map<Long, Vehicle> byId = vehicles.stream().filter(v -> v.getId() != null)
                        .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a));
                Map<Long, Long> ranked = getMostRentedVehicles(5);
                rows.add(new String[] {"RANKING"});
                rows.add(new String[] {"Rank", "Vehicle ID", "Brand", "Model", "Reservation count"});
                int rank = 1;
                for (Map.Entry<Long, Long> entry : ranked.entrySet()) {
                    Vehicle v = byId.get(entry.getKey());
                    rows.add(new String[] {String.valueOf(rank++), str(entry.getKey()), v == null ? "Unknown" : str(v.getBrand()),
                            v == null ? "Unknown" : str(v.getModel()), String.valueOf(entry.getValue())});
                }
            }
            case RATING_ANALYSIS -> {
                List<Review> reviews = getAllReviews();
                double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                rows.add(new String[] {"RATING SUMMARY"});
                rows.add(new String[] {"Metric", "Value"});
                rows.add(new String[] {"Total reviews", String.valueOf(reviews.size())});
                rows.add(new String[] {"Average rating out of 5", String.format(Locale.ROOT, "%.2f", average)});
                rows.add(new String[] {"Flagged reviews", String.valueOf(reviews.stream().filter(Review::isFlagged).count())});
                rows.add(new String[] {});
                rows.add(new String[] {"RATING DISTRIBUTION"});
                rows.add(new String[] {"Rating", "Review count", "Share (%)"});
                for (int rating = 5; rating >= 1; rating--) {
                    final int score = rating;
                    long count = reviews.stream().filter(r -> r.getRating() == score).count();
                    rows.add(new String[] {String.valueOf(rating), String.valueOf(count), String.format(Locale.ROOT, "%.1f", reviews.isEmpty() ? 0 : count * 100.0 / reviews.size())});
                }
                rows.add(new String[] {});
                rows.add(new String[] {"REVIEW DETAILS"});
                rows.add(new String[] {"Review ID", "Vehicle ID", "Customer ID", "Rating", "Flagged", "Management status", "Submitted at", "Comment"});
                reviews.stream().sorted(Comparator.comparing(Review::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .forEach(r -> rows.add(new String[] {str(r.getId()), str(r.getVehicleId()), str(r.getCustomerId()), String.valueOf(r.getRating()),
                                String.valueOf(r.isFlagged()), str(r.getManagementStatus()), str(r.getSubmittedAt()), str(r.getComment())}));
            }
        }
        StringBuilder csv = new StringBuilder("\uFEFF"); // UTF-8 BOM helps Excel detect Unicode correctly.
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) csv.append(',');
                csv.append(csvCell(row[i]));
            }
            csv.append("\r\n");
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        // Prevent spreadsheet formula execution when comments or labels begin with formula markers.
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String str(Object value) { return value == null ? "" : String.valueOf(value); }
    private String moneyCsv(BigDecimal value) { return value == null ? "0.00" : value.toPlainString(); }

    public List<ReportExportConfig> getExportHistory() {
        return reportExportConfigRepository.findAll();
    }
}
