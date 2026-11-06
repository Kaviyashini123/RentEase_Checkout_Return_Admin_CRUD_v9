package com.rentease.renteasesystem.reports.controller;

import com.rentease.renteasesystem.reports.model.ReportExportConfig;
import com.rentease.renteasesystem.auth.model.SessionUser;
import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.auth.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.rentease.renteasesystem.reports.model.Review;
import com.rentease.renteasesystem.reports.model.ReviewManagementUpdate;
import com.rentease.renteasesystem.reports.service.ReportReviewService;
import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MEMBER 6 - Kaviyashini V (IT25101787)
 * Reports & Reviews Management Module - Controller
 * Serves the Business Development Manager dashboard and customer review submission.
 */
@Controller
@RequestMapping("/reports")
public class ReportReviewController {

    private final ReportReviewService reportReviewService;
    private final UserRepository userRepository;

    public ReportReviewController(ReportReviewService reportReviewService, UserRepository userRepository) {
        this.reportReviewService = reportReviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String month, Model model) {
        YearMonth ym = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        BigDecimal revenue = reportReviewService.getMonthlyRevenue(ym);
        List<Vehicle> vehicles = reportReviewService.getOverviewVehicles();
        List<Reservation> reservations = reportReviewService.getOverviewReservations();
        List<Payment> payments = reportReviewService.getOverviewPayments();
        List<CheckoutRecord> checkouts = reportReviewService.getOverviewCheckoutRecords();
        double utilization = reportReviewService.getVehicleUtilizationRate();
        List<Review> reviews = reportReviewService.getAllReviews().stream()
                .sorted(Comparator.comparing(Review::getSubmittedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))).toList();
        Map<Long, String> vehicleNames = new LinkedHashMap<>();
        vehicles.forEach(vehicle -> vehicleNames.put(vehicle.getId(), vehicle.getBrand() + " " + vehicle.getModel()));

        Map<String, Long> fleetStatusCounts = reportReviewService.getFleetStatusCounts(vehicles);
        Map<String, Long> reservationStatusCounts = reportReviewService.getReservationStatusCounts(reservations);
        Map<String, Object> paymentStatusSummary = reportReviewService.getPaymentStatusSummary(payments);
        List<Map<String, Object>> fleetSegments = new ArrayList<>();
        double circumference = 2 * Math.PI * 48;
        double ringOffset = 0;
        for (Vehicle.VehicleStatus status : Vehicle.VehicleStatus.values()) {
            long count = fleetStatusCounts.getOrDefault(status.name(), 0L);
            double length = vehicles.isEmpty() ? 0 : circumference * count / vehicles.size();
            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("status", status.name());
            segment.put("label", status.name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
            segment.put("count", count);
            segment.put("dasharray", String.format(Locale.ROOT, "%.2f %.2f", length, circumference - length));
            segment.put("dashoffset", String.format(Locale.ROOT, "%.2f", -ringOffset));
            fleetSegments.add(segment);
            ringOffset += length;
        }

        List<Map<String, Object>> reservationTimeline = reportReviewService.getReservationTimeline(ym, 8, reservations);
        long maximumReservations = Math.max(1L, reservationTimeline.stream()
                .mapToLong(point -> ((Number) point.get("total")).longValue()).max().orElse(0L));
        for (Map<String, Object> point : reservationTimeline) {
            Map<String, Long> counts = (Map<String, Long>) point.get("counts");
            List<Map<String, Object>> segments = new ArrayList<>();
            for (Reservation.ReservationStatus status : Reservation.ReservationStatus.values()) {
                Map<String, Object> segment = new LinkedHashMap<>();
                segment.put("status", status.name());
                segment.put("count", counts.getOrDefault(status.name(), 0L));
                segments.add(segment);
            }
            point.put("segments", segments);
        }

        long activeRentals = checkouts.stream().filter(record -> record.getStage() == CheckoutRecord.RecordStage.HANDOVER).count();
        long paidReservations = payments.stream().filter(payment -> payment.getStatus() == Payment.PaymentStatus.SUCCESS)
                .map(Payment::getReservationId).distinct().count();
        BigDecimal paidTotal = payments.stream().filter(payment -> payment.getStatus() == Payment.PaymentStatus.SUCCESS)
                .map(Payment::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingTotal = payments.stream().filter(payment -> payment.getStatus() == Payment.PaymentStatus.PENDING)
                .map(Payment::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> paymentStatuses = java.util.Arrays.stream(Payment.PaymentStatus.values())
                .map(status -> (Map<String, Object>) paymentStatusSummary.get(status.name())).toList();
        List<Map<String, Object>> lifecycle = List.of(
                lifecycleStep("Reserved", reservationStatusCounts.getOrDefault("PENDING", 0L), "reservation"),
                lifecycleStep("Confirmed", reservationStatusCounts.getOrDefault("CONFIRMED", 0L), "reservation"),
                lifecycleStep("Paid", paidReservations, "payment"),
                lifecycleStep("Checked out", activeRentals, "checkout"),
                lifecycleStep("Active rental", activeRentals, "checkout"),
                lifecycleStep("Returned", checkouts.stream().filter(record -> record.getStage() == CheckoutRecord.RecordStage.RETURNED).count(), "return"),
                lifecycleStep("Completed", reservationStatusCounts.getOrDefault("COMPLETED", 0L), "completed"));

        model.addAttribute("month", ym.toString());
        model.addAttribute("revenue", revenue);
        model.addAttribute("utilization", utilization);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("reservations", reservations);
        model.addAttribute("payments", payments);
        model.addAttribute("revenueTimeline", reportReviewService.getRevenueTimeline(ym, 12));
        model.addAttribute("fleetSegments", fleetSegments);
        model.addAttribute("fleetStatusCounts", fleetStatusCounts);
        model.addAttribute("reservationStatusCounts", reservationStatusCounts);
        model.addAttribute("reservationStatuses", Reservation.ReservationStatus.values());
        model.addAttribute("reservationTimeline", reservationTimeline);
        model.addAttribute("maxReservations", maximumReservations);
        model.addAttribute("topVehicles", reportReviewService.getTopCompletedVehicles(5, reservations, vehicles));
        model.addAttribute("lifecycle", lifecycle);
        model.addAttribute("paymentStatuses", paymentStatuses);
        model.addAttribute("paymentCount", payments.size());
        model.addAttribute("paidTotal", paidTotal);
        model.addAttribute("pendingTotal", pendingTotal);
        model.addAttribute("activeRentals", activeRentals);
        model.addAttribute("availableVehicles", fleetStatusCounts.getOrDefault("AVAILABLE", 0L));
        model.addAttribute("recentActivity", reportReviewService.getRecentOperationsActivity(7, reservations, payments,
                checkouts, reviews, vehicles));
        double averageRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        long flaggedReviewCount = reviews.stream().filter(Review::isFlagged).count();
        long[] ratingCounts = new long[5];
        reviews.forEach(r -> {
            if (r.getRating() >= 1 && r.getRating() <= 5) ratingCounts[r.getRating() - 1]++;
        });
        model.addAttribute("reviews", reviews.stream().limit(3).toList());
        model.addAttribute("vehicleNames", vehicleNames);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("flaggedReviewCount", flaggedReviewCount);
        model.addAttribute("ratingCounts", ratingCounts);
        model.addAttribute("exportHistory", reportReviewService.getExportHistory());
        model.addAttribute("reportTypes", ReportExportConfig.ReportType.values());
        model.addAttribute("formats", ReportExportConfig.ExportFormat.values());
        return "reports/reports-dashboard";
    }

    /** Dedicated staff destination for the existing report export/history flow. */
    @GetMapping("/studio")
    public String reportStudio(@RequestParam(required = false) String month, Model model) {
        YearMonth reportingMonth = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        List<Review> reviews = reportReviewService.getAllReviews();
        double averageRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        List<ReportExportConfig> exportHistory = reportReviewService.getExportHistory().stream()
                .sorted(Comparator.comparing(ReportExportConfig::getGeneratedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();
        model.addAttribute("month", reportingMonth.toString());
        model.addAttribute("reportTypes", ReportExportConfig.ReportType.values());
        model.addAttribute("formats", ReportExportConfig.ExportFormat.values());
        model.addAttribute("exportHistory", exportHistory);
        model.addAttribute("studioRevenue", reportReviewService.getMonthlyRevenue(reportingMonth));
        model.addAttribute("studioUtilization", reportReviewService.getVehicleUtilizationRate());
        model.addAttribute("studioRankedVehicleCount", reportReviewService.getMostRentedVehicles(5).size());
        model.addAttribute("studioAverageRating", averageRating);
        model.addAttribute("studioReviewCount", reviews.size());
        return "reports/report-studio";
    }

    private static Map<String, Object> lifecycleStep(String label, long count, String kind) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("label", label);
        step.put("count", count);
        step.put("kind", kind);
        return step;
    }

    @PostMapping("/export")
    public Object export(@RequestParam ReportExportConfig.ReportType reportType,
                          @RequestParam ReportExportConfig.ExportFormat format,
                          @RequestParam String periodLabel,
                          @RequestParam String staffId,
                          @RequestParam(required = false) String returnTo,
                          jakarta.servlet.http.HttpServletRequest request,
                          RedirectAttributes attrs) {
        boolean wantsJson = request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
        String destination = switch (returnTo == null ? "" : returnTo) {
            case "reviews" -> "redirect:/reports/reviews#report-center";
            case "studio" -> "redirect:/reports/studio";
            default -> "redirect:/reports/dashboard";
        };
        try {
            ReportExportConfig config = reportReviewService.exportReport(reportType, format, periodLabel, staffId);
            if (wantsJson) {
                java.util.Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", true);
                res.put("id", config.getId());
                res.put("reportType", config.getReportType().name());
                res.put("periodLabel", config.getPeriodLabel());
                res.put("format", config.getFormat().name());
                String extension = config.getFormat().name().toLowerCase(Locale.ROOT);
                res.put("viewUrl", config.getFormat() == ReportExportConfig.ExportFormat.PDF
                        ? "/reports/export/" + config.getId() + "/pdf?preview=true" : null);
                res.put("downloadUrl", "/reports/export/" + config.getId() + "/" + extension);
                res.put("generatedAt", config.getGeneratedAt() != null ? config.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm")) : "");
                return ResponseEntity.ok(res);
            }
            attrs.addFlashAttribute("success", "Report export saved. Download the generated " + format.name() + " from the report history.");
            attrs.addFlashAttribute("generatedExportId", config.getId());
            attrs.addFlashAttribute("generatedExportFormat", format);
        } catch (IllegalArgumentException exception) {
            if (wantsJson) {
                java.util.Map<String, Object> err = new java.util.HashMap<>();
                err.put("success", false);
                err.put("message", exception.getMessage());
                return ResponseEntity.badRequest().body(err);
            }
            attrs.addFlashAttribute("exportError", exception.getMessage());
            attrs.addFlashAttribute("exportType", reportType);
            attrs.addFlashAttribute("exportPeriod", periodLabel);
            attrs.addFlashAttribute("exportStaffId", staffId);
        }
        return destination;
    }

    @GetMapping("/export/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id,
                                              @RequestParam(required = false, defaultValue = "false") boolean preview) {
        ReportExportConfig config = reportReviewService.getExport(id);
        byte[] pdf = reportReviewService.generatePdf(id);
        String filename = config.getReportType().name().toLowerCase() + "_" + config.getPeriodLabel() + ".pdf";
        String dispositionType = preview ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/export/{id}/csv")
    public ResponseEntity<byte[]> downloadCsv(@PathVariable Long id) {
        ReportExportConfig config = reportReviewService.getExport(id);
        if (config.getFormat() != ReportExportConfig.ExportFormat.CSV) {
            return ResponseEntity.badRequest().build();
        }
        byte[] csv = reportReviewService.generateCsv(id);
        String filename = config.getReportType().name().toLowerCase(Locale.ROOT) + "_" + config.getPeriodLabel() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .contentLength(csv.length)
                .body(csv);
    }

    @GetMapping("/review")
    public String reviewForm(@RequestParam(required = false) Long vehicleId,
                             @RequestParam(required = false) Long reservationId,
                             Model model, HttpSession session) {
        SessionUser current = currentUser(session);
        if (!current.isAdmin()) {
            if (reservationId == null) {
                List<Reservation> eligible = reportReviewService.getReservationsAwaitingReview(current.getId());
                if (eligible.size() == 1) {
                    return "redirect:/reports/review?reservationId=" + eligible.get(0).getId();
                }
                addReviewSelection(model, eligible);
                return "reports/review-selection";
            }

            Reservation reservation = reservationOr404(reservationId);
            if (!current.getId().equals(reservation.getCustomerId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "You cannot review another customer's reservation.");
            }
            Vehicle vehicle = vehicleOrNull(reservation.getVehicleId());
            model.addAttribute("reservation", reservation);
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleImage", resolveVehicleImage(vehicle));
            boolean reviewed = reportReviewService.hasReviewForReservation(reservationId, current.getId());
            if (reviewed) {
                model.addAttribute("feedbackAlreadyReceived", !model.containsAttribute("feedbackReceived"));
                model.addAttribute("feedbackReceived", model.containsAttribute("feedbackReceived"));
                return "reports/review-success";
            }
            if (reservation.getStatus() != Reservation.ReservationStatus.COMPLETED
                    || reportReviewService.hasReviewForReservation(reservationId)) {
                List<Reservation> eligible = reportReviewService.getReservationsAwaitingReview(current.getId());
                model.addAttribute("selectionNotice", "Feedback is available for completed journeys that have not already been reviewed.");
                addReviewSelection(model, eligible);
                return "reports/review-selection";
            }

            Review review = new Review();
            review.setReservationId(reservation.getId());
            review.setVehicleId(reservation.getVehicleId());
            model.addAttribute("review", review);
            model.addAttribute("existingReviews", reportReviewService.getReviewsForVehicle(reservation.getVehicleId()));
            model.addAttribute("averageRating", reportReviewService.getAverageRatingForVehicle(reservation.getVehicleId()));
            return "reports/review-form";
        }

        Review review = new Review();
        review.setVehicleId(vehicleId);
        model.addAttribute("review", review);
        if (vehicleId != null) {
            Vehicle vehicle = vehicleOrNull(vehicleId);
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleImage", resolveVehicleImage(vehicle));
            model.addAttribute("existingReviews", reportReviewService.getReviewsForVehicle(vehicleId));
            model.addAttribute("averageRating", reportReviewService.getAverageRatingForVehicle(vehicleId));
        }
        return "reports/review-form";
    }

    @PostMapping("/review")
    public String submitReview(@ModelAttribute Review review, HttpSession session, RedirectAttributes attrs) {
        SessionUser current = currentUser(session);
        if (!current.isAdmin()) {
            if (review.getReservationId() == null) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Choose a completed reservation before submitting feedback.");
            }
            Reservation reservation = reservationOr404(review.getReservationId());
            if (!current.getId().equals(reservation.getCustomerId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "You cannot review another customer's reservation.");
            }
            if (reservation.getStatus() != Reservation.ReservationStatus.COMPLETED) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "Only completed journeys are eligible for feedback.");
            }
            if (reportReviewService.hasReviewForReservation(reservation.getId())) {
                attrs.addFlashAttribute("feedbackAlreadyReceived", true);
                return "redirect:/reports/review?reservationId=" + reservation.getId();
            }
            review.setCustomerId(current.getId());
            review.setVehicleId(reservation.getVehicleId());
        }
        reportReviewService.submitReview(review);
        if (!current.isAdmin()) {
            attrs.addFlashAttribute("feedbackReceived", true);
            return "redirect:/reports/review?reservationId=" + review.getReservationId();
        }
        return "redirect:/reports/review?vehicleId=" + review.getVehicleId();
    }

    private void addReviewSelection(Model model, List<Reservation> reservations) {
        Map<Long, Vehicle> vehiclesById = new LinkedHashMap<>();
        Map<Long, String> vehicleImagesById = new LinkedHashMap<>();
        reservations.forEach(reservation -> {
            Vehicle vehicle = vehicleOrNull(reservation.getVehicleId());
            if (vehicle != null) {
                vehiclesById.put(reservation.getVehicleId(), vehicle);
                vehicleImagesById.put(reservation.getVehicleId(), resolveVehicleImage(vehicle));
            }
        });
        model.addAttribute("eligibleReservations", reservations);
        model.addAttribute("vehiclesById", vehiclesById);
        model.addAttribute("vehicleImagesById", vehicleImagesById);
    }

    private Reservation reservationOr404(Long reservationId) {
        try { return reportReviewService.getReservation(reservationId); }
        catch (IllegalArgumentException ignored) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "Reservation not found.");
        }
    }

    private Vehicle vehicleOrNull(Long vehicleId) {
        try { return reportReviewService.getVehicle(vehicleId); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    // ---- Reports UI CRUD: reviews are managed inside Reports ----
    @GetMapping("/reviews") public String reviews(Model model) {
        List<Review> reviews = reportReviewService.getAllReviews().stream()
                .sorted(Comparator.comparing(Review::getSubmittedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();
        List<Vehicle> vehicles = reportReviewService.getOverviewVehicles();
        List<Reservation> reservations = reportReviewService.getOverviewReservations();
        List<User> customers = userRepository.findAll();
        Map<Long, Vehicle> vehiclesById = vehicles.stream().filter(v -> v.getId() != null)
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a, LinkedHashMap::new));
        Map<Long, User> customersById = customers.stream().filter(c -> c.getId() != null)
                .collect(Collectors.toMap(User::getId, c -> c, (a, b) -> a, LinkedHashMap::new));
        Map<Long, Reservation> reservationsById = reservations.stream().filter(r -> r.getId() != null)
                .collect(Collectors.toMap(Reservation::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
        Map<Long, Vehicle> reviewVehiclesById = new LinkedHashMap<>();
        Map<Long, Long> reviewVehicleIdsById = new LinkedHashMap<>();
        Map<Long, String> reviewVehicleImagesById = new LinkedHashMap<>();
        Map<Long, Reservation> reviewReservationsById = new LinkedHashMap<>();
        for (Review review : reviews) {
            if (review.getId() == null) continue;
            Vehicle vehicle = null;
            boolean trustedReservationLink = false;
            Long trustedReservationVehicleId = null;
            Reservation linkedReservation = review.getReservationId() == null
                    ? null : reservationsById.get(review.getReservationId());
            if (isReservationOwnedByReviewCustomer(review, linkedReservation)) {
                trustedReservationLink = true;
                reviewReservationsById.put(review.getId(), linkedReservation);
                trustedReservationVehicleId = linkedReservation.getVehicleId();
            }
            vehicle = resolveReviewVehicle(review, linkedReservation, vehiclesById);
            if (vehicle != null) {
                reviewVehiclesById.put(review.getId(), vehicle);
                reviewVehicleIdsById.put(review.getId(), vehicle.getId());
                String image = resolveVehicleImage(vehicle);
                if (image != null && !image.isBlank()) reviewVehicleImagesById.put(review.getId(), image);
            } else {
                reviewVehicleIdsById.put(review.getId(), trustedReservationLink
                        ? trustedReservationVehicleId : review.getVehicleId());
            }
        }

        double averageRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        long fiveStarReviewCount = reviews.stream().filter(r -> r.getRating() == 5).count();
        long attentionCount = reviews.stream().filter(r -> r.isFlagged() || r.getRating() <= 2).count();
        long positiveReviewCount = reviews.stream().filter(r -> r.getRating() >= 4).count();
        double positiveExperienceRate = reviews.isEmpty() ? 0 : positiveReviewCount * 100.0 / reviews.size();
        double attentionRate = reviews.isEmpty() ? 0 : attentionCount * 100.0 / reviews.size();
        // Deterministic health index: 60% rating quality, 30% positive (4–5 star) share,
        // and 10% inverse attention share; every input is derived from stored reviews.
        double ratingQualityRate = averageRating * 20.0;
        double experienceHealthScore = reviews.isEmpty() ? 0 : Math.round(
                (ratingQualityRate * 0.60) + (positiveExperienceRate * 0.30) + ((100.0 - attentionRate) * 0.10));
        String experienceHealthLabel = reviews.isEmpty() ? "Awaiting feedback"
                : experienceHealthScore >= 85 ? "Excellent"
                : experienceHealthScore >= 70 ? "Strong"
                : experienceHealthScore >= 55 ? "Developing" : "Needs attention";
        long[] ratingCounts = new long[5];
        reviews.forEach(r -> { if (r.getRating() >= 1 && r.getRating() <= 5) ratingCounts[r.getRating() - 1]++; });
        List<Map<String, Object>> ratingDistribution = new ArrayList<>();
        for (int rating = 5; rating >= 1; rating--) {
            long count = ratingCounts[rating - 1];
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rating", rating);
            row.put("count", count);
            row.put("percent", reviews.isEmpty() ? 0 : Math.round(count * 100.0 / reviews.size()));
            ratingDistribution.add(row);
        }

        Set<Long> attentionVehicleIds = new java.util.HashSet<>(
                reportReviewService.getCompletedRentalVehicleIdsNeedingAttention(3.0));
        reviews.stream().filter(review -> review.isFlagged() || review.getRating() <= 2)
                .map(review -> reviewVehicleIdsById.getOrDefault(review.getId(), review.getVehicleId()))
                .filter(java.util.Objects::nonNull).forEach(attentionVehicleIds::add);
        List<Map<String, Object>> vehicleExperience = reportReviewService.getCompletedRentalVehicleRatingSummaries().stream().map(summary -> {
            Long vehicleId = ((Number) summary[0]).longValue();
            double average = ((Number) summary[4]).doubleValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", vehicleId);
            row.put("name", ((summary[1] == null ? "" : summary[1]) + " " + (summary[2] == null ? "" : summary[2])).trim());
            row.put("category", summary[3] == null ? "Vehicle" : summary[3].toString().replace('_', ' '));
            Vehicle vehicle = vehiclesById.get(vehicleId);
            row.put("image", resolveVehicleImage(vehicle));
            row.put("average", average);
            row.put("count", ((Number) summary[5]).longValue());
            row.put("minimum", ((Number) summary[6]).intValue());
            row.put("maximum", ((Number) summary[7]).intValue());
            row.put("percent", Math.round(average * 20));
            boolean attention = attentionVehicleIds.contains(vehicleId) || average <= 3.0;
            row.put("attention", attention);
            row.put("experienceStatus", attention ? "Needs attention" : average >= 4.5 ? "Excellent"
                    : average >= 4.0 ? "Strong" : "Monitor");
            return row;
        }).toList();

        Map<YearMonth, List<Review>> reviewsByMonth = reviews.stream().filter(r -> r.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getSubmittedAt())));
        YearMonth currentReviewMonth = YearMonth.now();
        YearMonth previousReviewMonth = currentReviewMonth.minusMonths(1);
        List<Review> currentPeriodReviews = reviewsByMonth.getOrDefault(currentReviewMonth, List.of());
        List<Review> previousPeriodReviews = reviewsByMonth.getOrDefault(previousReviewMonth, List.of());
        double currentPeriodAverage = currentPeriodReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        double previousPeriodAverage = previousPeriodReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        List<Map<String, Object>> reviewTrend = reviewsByMonth.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", entry.getKey().toString());
                    row.put("label", entry.getKey().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                            + " '" + String.valueOf(entry.getKey().getYear()).substring(2));
                    row.put("average", entry.getValue().stream().mapToInt(Review::getRating).average().orElse(0.0));
                    row.put("count", entry.getValue().size());
                    return row;
                }).toList();
        StringBuilder reviewTrendPath = new StringBuilder();
        for (int index = 0; index < reviewTrend.size(); index++) {
            Map<String, Object> point = reviewTrend.get(index);
            double x = reviewTrend.size() == 1 ? 374 : 54 + index * (640.0 / (reviewTrend.size() - 1));
            double y = 190 - ((double) point.get("average") / 5.0 * 150);
            point.put("x", Math.round(x * 10) / 10.0);
            point.put("y", Math.round(y * 10) / 10.0);
            if (index > 0) reviewTrendPath.append(' ');
            reviewTrendPath.append(index == 0 ? 'M' : 'L').append(point.get("x")).append(' ').append(point.get("y"));
        }

        Map<Long, String> vehicleImagesById = new LinkedHashMap<>();
        vehicles.forEach(v -> vehicleImagesById.put(v.getId(), resolveVehicleImage(v)));
        model.addAttribute("reviews", reviews);
        model.addAttribute("vehiclesById", vehiclesById);
        model.addAttribute("vehicleImagesById", vehicleImagesById);
        model.addAttribute("customersById", customersById);
        model.addAttribute("reservationsById", reservationsById);
        model.addAttribute("reviewVehiclesById", reviewVehiclesById);
        model.addAttribute("reviewVehicleIdsById", reviewVehicleIdsById);
        model.addAttribute("reviewVehicleImagesById", reviewVehicleImagesById);
        model.addAttribute("reviewReservationsById", reviewReservationsById);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("fiveStarReviewCount", fiveStarReviewCount);
        model.addAttribute("fiveStarReviewPercent", reviews.isEmpty() ? 0 : Math.round(fiveStarReviewCount * 100.0 / reviews.size()));
        model.addAttribute("flaggedReviewCount", reviews.stream().filter(Review::isFlagged).count());
        model.addAttribute("attentionCount", attentionCount);
        model.addAttribute("positiveReviewCount", positiveReviewCount);
        model.addAttribute("ratingQualityRate", Math.round(ratingQualityRate));
        model.addAttribute("positiveExperienceRate", Math.round(positiveExperienceRate));
        model.addAttribute("attentionRate", Math.round(attentionRate));
        model.addAttribute("experienceHealthScore", (int) experienceHealthScore);
        model.addAttribute("experienceHealthLabel", experienceHealthLabel);
        model.addAttribute("ratingDistribution", ratingDistribution);
        model.addAttribute("vehicleExperience", vehicleExperience);
        model.addAttribute("bestRatedVehicleId", vehicleExperience.isEmpty() ? null : vehicleExperience.get(0).get("vehicleId"));
        model.addAttribute("reviewTrend", reviewTrend);
        model.addAttribute("reviewTrendPath", reviewTrendPath.toString());
        model.addAttribute("trendReady", reviewsByMonth.size() >= 2);
        model.addAttribute("currentReviewPeriod", currentReviewMonth.toString());
        model.addAttribute("previousReviewPeriod", previousReviewMonth.toString());
        model.addAttribute("currentPeriodReviewCount", currentPeriodReviews.size());
        model.addAttribute("previousPeriodReviewCount", previousPeriodReviews.size());
        model.addAttribute("currentPeriodAverage", currentPeriodAverage);
        model.addAttribute("previousPeriodAverage", previousPeriodAverage);
        model.addAttribute("reviewPeriodComparisonAvailable", !currentPeriodReviews.isEmpty() && !previousPeriodReviews.isEmpty());
        model.addAttribute("reviewPeriodAverageDelta", Math.round((currentPeriodAverage - previousPeriodAverage) * 100.0) / 100.0);
        model.addAttribute("reviewReportPeriod", YearMonth.now().toString());
        model.addAttribute("reportTypes", ReportExportConfig.ReportType.values());
        model.addAttribute("managementStatuses", Review.ManagementStatus.values());
        model.addAttribute("exportHistory", reportReviewService.getExportHistory());
        return "reports/reviews";
    }
    @PostMapping("/review/management/{id}")
    public String updateReviewManagement(@PathVariable Long id,
                                         @Valid @ModelAttribute ReviewManagementUpdate update,
                                         BindingResult bindingResult,
                                         RedirectAttributes attrs) {
        if (bindingResult.hasErrors()) {
            attrs.addFlashAttribute("managementError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/reports/reviews#review-" + id;
        }
        try {
            reportReviewService.updateReviewManagement(id, update.getManagementStatus(), update.getManagementNote());
            attrs.addFlashAttribute("managementSuccess", "Review management details saved.");
        } catch (IllegalArgumentException exception) {
            attrs.addFlashAttribute("managementError", exception.getMessage());
        }
        return "redirect:/reports/reviews#review-" + id;
    }

    @GetMapping("/review/edit/{id}") public String editReview(@PathVariable Long id, Model model) { model.addAttribute("review", reportReviewService.getAllReviews().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Review not found"))); return "reports/review-form"; }
    @PostMapping("/review/edit/{id}") public String updateReview(@PathVariable Long id, @ModelAttribute Review review, RedirectAttributes attrs) { reportReviewService.updateReview(id, review); attrs.addFlashAttribute("success", "Review updated successfully."); return "redirect:/reports/reviews"; }
    @PostMapping("/review/delete/{id}") public String deleteReview(@PathVariable Long id, RedirectAttributes attrs) { reportReviewService.deleteReview(id); attrs.addFlashAttribute("success", "Review deleted successfully."); return "redirect:/reports/reviews"; }

    // ---- JSON API ----

    @RestController
    @RequestMapping("/api/reports")
    public static class ReportReviewApiController {

        private final ReportReviewService reportReviewService;

        public ReportReviewApiController(ReportReviewService reportReviewService) {
            this.reportReviewService = reportReviewService;
        }

        @GetMapping("/revenue")
        public ResponseEntity<BigDecimal> revenue(@RequestParam String month, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.getMonthlyRevenue(YearMonth.parse(month)));
        }

        @GetMapping("/utilization")
        public ResponseEntity<Double> utilization(HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.getVehicleUtilizationRate());
        }

        @GetMapping("/most-rented")
        public ResponseEntity<Map<Long, Long>> mostRented(@RequestParam(defaultValue = "5") int topN, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.getMostRentedVehicles(topN));
        }

        @GetMapping("/ratings/{vehicleId}")
        public ResponseEntity<Double> ratings(@PathVariable Long vehicleId) {
            return ResponseEntity.ok(reportReviewService.getAverageRatingForVehicle(vehicleId));
        }

        @PostMapping("/reviews")
        public ResponseEntity<Review> submitReview(@RequestBody Review review, HttpSession session) {
            SessionUser current = currentUser(session);
            if (!current.isAdmin()) review.setCustomerId(current.getId());
            return ResponseEntity.ok(reportReviewService.submitReview(review));
        }

        @GetMapping("/reviews")
        public ResponseEntity<List<Review>> allReviews(HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.getAllReviews());
        }

        @PostMapping("/export")
        public ResponseEntity<ReportExportConfig> export(@RequestParam ReportExportConfig.ReportType reportType,
                                                           @RequestParam ReportExportConfig.ExportFormat format,
                                                           @RequestParam String periodLabel,
                                                           @RequestParam String staffId,
                                                           HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.exportReport(reportType, format, periodLabel, staffId));
        }

        @PutMapping("/reviews/{id}")
        public ResponseEntity<Review> updateReview(@PathVariable Long id, @Valid @RequestBody Review review, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.updateReview(id, review));
        }

        @DeleteMapping("/reviews/{id}")
        public ResponseEntity<Void> deleteReview(@PathVariable Long id, HttpSession session) {
            requireAdmin(session);
            reportReviewService.deleteReview(id);
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/exports")
        public ResponseEntity<List<ReportExportConfig>> allExports(HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.getExportHistory());
        }

        @GetMapping("/exports/{id}")
        public ResponseEntity<ReportExportConfig> getExport(@PathVariable Long id, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.getExport(id));
        }

        @PutMapping("/exports/{id}")
        public ResponseEntity<ReportExportConfig> updateExport(@PathVariable Long id, @RequestBody ReportExportConfig config, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reportReviewService.updateExport(id, config));
        }

        @DeleteMapping("/exports/{id}")
        public ResponseEntity<Void> deleteExport(@PathVariable Long id, HttpSession session) {
            requireAdmin(session);
            reportReviewService.deleteExport(id);
            return ResponseEntity.noContent().build();
        }
    }

    private static SessionUser currentUser(HttpSession session) {
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (current == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return current;
    }

    private static void requireAdmin(HttpSession session) {
        if (!currentUser(session).isAdmin()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin access required.");
        }
    }

    static boolean isReservationOwnedByReviewCustomer(Review review, Reservation reservation) {
        return review != null && review.getReservationId() != null && reservation != null
                && java.util.Objects.equals(review.getReservationId(), reservation.getId())
                && review.getCustomerId() != null
                && reservation.getCustomerId() != null
                && java.util.Objects.equals(reservation.getCustomerId(), review.getCustomerId());
    }

    static Vehicle resolveReviewVehicle(Review review, Reservation linkedReservation,
                                        Map<Long, Vehicle> vehiclesById) {
        if (review == null || vehiclesById == null) return null;
        if (isReservationOwnedByReviewCustomer(review, linkedReservation)) {
            return vehiclesById.get(linkedReservation.getVehicleId());
        }
        // A missing or foreign reservation cannot provide journey information;
        // the review's independently stored vehicle reference remains valid.
        return vehiclesById.get(review.getVehicleId());
    }

    public static String resolveVehicleImage(Vehicle vehicle) {
        if (vehicle == null) return null;
        String storedImage = vehicle.getImageUrl() == null ? "" : vehicle.getImageUrl().trim();
        if (storedImage.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(storedImage);
            String scheme = uri.getScheme();
            if (uri.isAbsolute()) {
                return uri.getHost() != null
                        && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                        ? storedImage : null;
            }
            if (uri.getRawAuthority() != null || uri.getRawPath() == null || uri.getRawPath().isBlank()
                    || storedImage.indexOf('\\') >= 0) return null;
            String decodedPath = uri.getPath();
            if (decodedPath == null || java.util.Arrays.stream(decodedPath.split("/"))
                    .anyMatch(segment -> segment.equals(".."))) return null;
            String path = uri.getRawPath().startsWith("/") ? uri.getRawPath() : "/" + uri.getRawPath();
            return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                    + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}
