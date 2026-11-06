package com.rentease.renteasesystem.reservation.controller;

import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.auth.model.SessionUser;
import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.auth.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.rentease.renteasesystem.reservation.service.ReservationService;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.booking.service.VehicleSearchService;
import com.rentease.renteasesystem.reports.service.ReportReviewService;
import com.rentease.renteasesystem.payment.model.Invoice;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.payment.service.PaymentService;
import com.rentease.renteasesystem.checkout.service.CheckoutReturnService;
import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

/**
 * MEMBER 2 - Dilmani J.A.S. (IT25101845)
 * Booking & Reservation Management Module - Controller
 * Serves the supervisor dashboard (Thymeleaf) and a JSON API for booking actions.
 */
@Controller
@RequestMapping("/reservation")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReportReviewService reportReviewService;
    private final PaymentService paymentService;
    private final CheckoutReturnService checkoutReturnService;
    private final VehicleSearchService vehicleSearchService;
    private final UserRepository userRepository;

    public ReservationController(ReservationService reservationService, ReportReviewService reportReviewService,
                                 PaymentService paymentService, CheckoutReturnService checkoutReturnService,
                                 VehicleSearchService vehicleSearchService, UserRepository userRepository) {
        this.reservationService = reservationService;
        this.reportReviewService = reportReviewService;
        this.paymentService = paymentService;
        this.checkoutReturnService = checkoutReturnService;
        this.vehicleSearchService = vehicleSearchService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        SessionUser current = currentUser(session);
        List<Reservation> reservations = current.isAdmin()
                ? reservationService.getAllReservations()
                : reservationService.getReservationsForCustomer(current.getId());
        model.addAttribute("reservations", reservations);
        Set<Long> activeRentalReservationIds = new HashSet<>();
        List<CheckoutRecord> activeRecords = checkoutReturnService.getPendingReturns();
        for (CheckoutRecord record : activeRecords) {
            activeRentalReservationIds.add(record.getReservationId());
        }
        model.addAttribute("activeRentalReservationIds", activeRentalReservationIds);

        if (current.isAdmin()) {
            Map<String, Long> statusCounts = new HashMap<>();
            for (Reservation.ReservationStatus status : Reservation.ReservationStatus.values()) statusCounts.put(status.name(), 0L);
            for (Reservation reservation : reservations) {
                if (reservation.getStatus() != null) statusCounts.merge(reservation.getStatus().name(), 1L, Long::sum);
            }

            Map<Long, Vehicle> vehiclesById = new HashMap<>();
            for (Vehicle vehicle : vehicleSearchService.getAllVehicles()) vehiclesById.put(vehicle.getId(), vehicle);
            Map<Long, User> customersById = new HashMap<>();
            for (User customer : userRepository.findAll()) customersById.put(customer.getId(), customer);

            Map<Long, Payment> paymentsByReservation = new HashMap<>();
            for (Payment payment : paymentService.getAllPayments()) {
                Payment previous = paymentsByReservation.get(payment.getReservationId());
                if (previous == null || (payment.getId() != null && previous.getId() != null && payment.getId() > previous.getId())) {
                    paymentsByReservation.put(payment.getReservationId(), payment);
                }
            }
            Map<Long, CheckoutRecord> checkoutsByReservation = new HashMap<>();
            for (CheckoutRecord record : checkoutReturnService.getAllRecords()) {
                checkoutsByReservation.put(record.getReservationId(), record);
            }

            Map<Long, Vehicle> reservationVehicles = new HashMap<>();
            Map<Long, User> reservationCustomers = new HashMap<>();
            Map<Long, String> reservationVehicleImages = new HashMap<>();
            for (Reservation reservation : reservations) {
                Vehicle vehicle = vehiclesById.get(reservation.getVehicleId());
                if (vehicle != null) {
                    reservationVehicles.put(reservation.getId(), vehicle);
                    String localImage = localVehicleImage(vehicle);
                    if (localImage != null) reservationVehicleImages.put(reservation.getId(), localImage);
                }
                User customer = customersById.get(reservation.getCustomerId());
                if (customer != null) reservationCustomers.put(reservation.getId(), customer);
            }

            model.addAttribute("reservationStatusCounts", statusCounts);
            model.addAttribute("reservationStatuses", Reservation.ReservationStatus.values());
            model.addAttribute("paymentStatuses", Payment.PaymentStatus.values());
            model.addAttribute("reservationActiveCount", activeRentalReservationIds.size());
            model.addAttribute("reservationPaidCount", reservations.stream()
                    .map(reservation -> paymentsByReservation.get(reservation.getId()))
                    .filter(payment -> payment != null && payment.getStatus() == Payment.PaymentStatus.SUCCESS).count());
            model.addAttribute("reservationCurrentDate", LocalDate.now());
            model.addAttribute("reservationVehicles", reservationVehicles);
            model.addAttribute("reservationCustomers", reservationCustomers);
            model.addAttribute("reservationPayments", paymentsByReservation);
            model.addAttribute("reservationCheckouts", checkoutsByReservation);
            model.addAttribute("reservationVehicleImages", reservationVehicleImages);
        }
        Map<Long, Payment> successfulPayments = new HashMap<>();
        Map<Long, Invoice> invoicesByReservation = new HashMap<>();
        if (!current.isAdmin()) {
            for (Reservation reservation : reservations) {
                Payment paid = paymentService.getSuccessfulPaymentForReservation(reservation.getId());
                if (paid != null) {
                    successfulPayments.put(reservation.getId(), paid);
                    Invoice invoice = paymentService.findInvoiceByPayment(paid.getId());
                    if (invoice != null) invoicesByReservation.put(reservation.getId(), invoice);
                }
            }
        }
        model.addAttribute("successfulPayments", successfulPayments);
        model.addAttribute("invoicesByReservation", invoicesByReservation);
        model.addAttribute("reviewedReservationIds", current.isAdmin()
                ? Set.of()
                : reportReviewService.getReviewedReservationIds(current.getId()));
        return "reservation/dashboard";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {
        requireAdmin(session);
        model.addAttribute("reservation", reservationService.getReservation(id));
        return "reservation/reservation-form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Reservation reservation, Model model, RedirectAttributes attrs, HttpSession session) {
        requireAdmin(session);
        try { reservationService.updateReservation(id, reservation); attrs.addFlashAttribute("success", "Reservation updated successfully."); return "redirect:/reservation/dashboard"; }
        catch (IllegalArgumentException | IllegalStateException ex) { model.addAttribute("error", ex.getMessage()); reservation.setId(id); return "reservation/reservation-form"; }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs, HttpSession session) {
        requireAdmin(session);
        reservationService.deleteReservation(id); attrs.addFlashAttribute("success", "Reservation deleted successfully."); return "redirect:/reservation/dashboard";
    }

    @GetMapping("/new")
    public String newBookingForm(@RequestParam(required = false) Long vehicleId, Model model) {
        Reservation reservation = new Reservation();
        reservation.setVehicleId(vehicleId);
        model.addAttribute("reservation", reservation);
        return "reservation/reservation-form";
    }

    @PostMapping("/new")
    public String submitBooking(@ModelAttribute Reservation reservation, Model model, HttpSession session) {
        SessionUser current = currentUser(session);
        if (!current.isAdmin()) reservation.setCustomerId(current.getId());
        try {
            reservationService.createReservation(reservation);
            return "redirect:/reservation/dashboard";
        } catch (IllegalStateException | IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("reservation", reservation);
            return "reservation/reservation-form";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, @RequestParam String reason, HttpSession session) {
        SessionUser current = currentUser(session);
        Reservation existing = reservationService.getReservation(id);
        if (!current.isAdmin() && !current.getId().equals(existing.getCustomerId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You cannot cancel another customer's reservation.");
        }
        CheckoutRecord checkout = checkoutReturnService.findByReservation(id);
        if (checkout != null && checkout.getStage() == CheckoutRecord.RecordStage.HANDOVER) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "An active rental must be returned before it can be closed.");
        }
        reservationService.cancelReservation(id, reason);
        return "redirect:/reservation/dashboard";
    }

    // ---- JSON API ----

    @RestController
    @RequestMapping("/api/reservation")
    public static class ReservationApiController {

        private final ReservationService reservationService;
        private final CheckoutReturnService checkoutReturnService;

        public ReservationApiController(ReservationService reservationService, CheckoutReturnService checkoutReturnService) {
            this.reservationService = reservationService;
            this.checkoutReturnService = checkoutReturnService;
        }

        @GetMapping("/availability")
        public ResponseEntity<Boolean> checkAvailability(@RequestParam Long vehicleId,
                                                           @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
            return ResponseEntity.ok(reservationService.isVehicleAvailable(vehicleId, startDate, endDate));
        }

        @PostMapping
        public ResponseEntity<Reservation> create(@RequestBody Reservation reservation, HttpSession session) {
            SessionUser current = currentUser(session);
            if (!current.isAdmin()) reservation.setCustomerId(current.getId());
            return ResponseEntity.ok(reservationService.createReservation(reservation));
        }

        @PutMapping("/{id}/confirm")
        public ResponseEntity<Reservation> confirm(@PathVariable Long id, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reservationService.confirmReservation(id));
        }

        @PutMapping("/{id}/dates")
        public ResponseEntity<Reservation> updateDates(@PathVariable Long id,
                                                         @RequestParam LocalDate startDate,
                                                         @RequestParam LocalDate endDate) {
            return ResponseEntity.ok(reservationService.updateReservationDates(id, startDate, endDate));
        }

        @PutMapping("/{id}/vehicle")
        public ResponseEntity<Reservation> reassignVehicle(@PathVariable Long id, @RequestParam Long vehicleId, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reservationService.reassignVehicle(id, vehicleId));
        }

        @PutMapping("/{id}/cancel")
        public ResponseEntity<Reservation> cancel(@PathVariable Long id, @RequestParam String reason, HttpSession session) {
            Reservation existing = reservationService.getReservation(id);
            SessionUser current = currentUser(session);
            if (!current.isAdmin() && !current.getId().equals(existing.getCustomerId())) return ResponseEntity.status(403).build();
            CheckoutRecord checkout = checkoutReturnService.findByReservation(id);
            if (checkout != null && checkout.getStage() == CheckoutRecord.RecordStage.HANDOVER) return ResponseEntity.status(409).build();
            return ResponseEntity.ok(reservationService.cancelReservation(id, reason));
        }

        @GetMapping
        public ResponseEntity<List<Reservation>> all(HttpSession session) {
            SessionUser current = currentUser(session);
            return ResponseEntity.ok(current.isAdmin() ? reservationService.getAllReservations() : reservationService.getReservationsForCustomer(current.getId()));
        }

        @GetMapping("/{id}")
        public ResponseEntity<Reservation> get(@PathVariable Long id, HttpSession session) {
            Reservation reservation = reservationService.getReservation(id);
            SessionUser current = currentUser(session);
            if (!current.isAdmin() && !current.getId().equals(reservation.getCustomerId())) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(reservation);
        }

        // Reservation module CRUD
        @PutMapping("/{id}")
        public ResponseEntity<Reservation> update(@PathVariable Long id, @RequestBody Reservation reservation, HttpSession session) {
            requireAdmin(session);
            return ResponseEntity.ok(reservationService.updateReservation(id, reservation));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
            requireAdmin(session);
            reservationService.deleteReservation(id);
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

    /** Use only known local vehicle photography in the operations view. */
    private static String localVehicleImage(Vehicle vehicle) {
        if (vehicle.getImageUrl() != null && vehicle.getImageUrl().startsWith("/images/vehicles/")) {
            return vehicle.getImageUrl();
        }
        String brand = vehicle.getBrand() == null ? "" : vehicle.getBrand().trim().toLowerCase();
        String name = vehicle.getModel() == null ? "" : vehicle.getModel().trim().toLowerCase();
        if (brand.equals("honda") && name.equals("vezel")) return "/images/vehicles/honda-vezel.jpg";
        if (brand.equals("bmw") && name.contains("5")) return "/images/vehicles/bmw-5-series.jpg";
        if (brand.equals("tesla") && name.contains("3")) return "/images/vehicles/tesla-model-3.jpg";
        if (brand.equals("toyota") && name.contains("prius")) return "/images/vehicles/toyota-prius.jpg";
        if (brand.equals("toyota") && name.contains("hiace")) return "/images/vehicles/toyota-hiace.jpg";
        if (brand.equals("toyota") && name.contains("corolla")) return "/images/vehicles/toyota-corolla-axio.jpg";
        if (brand.equals("suzuki") && name.contains("wagon")) return "/images/vehicles/suzuki-wagon-r.png";
        if (brand.equals("nissan") && name.contains("caravan")) return "/images/vehicles/nissan-caravan.jpg";
        if (brand.equals("yamaha") && name.contains("fz")) return "/images/vehicles/yamaha-fz-s.jpg";
        return null;
    }
}
