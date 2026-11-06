package com.rentease.renteasesystem.checkout.controller;

import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import com.rentease.renteasesystem.checkout.model.DamageReport;
import com.rentease.renteasesystem.checkout.service.CheckoutReturnService;
import com.rentease.renteasesystem.auth.model.SessionUser;
import com.rentease.renteasesystem.auth.repository.UserRepository;
import com.rentease.renteasesystem.booking.service.VehicleSearchService;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.payment.service.PaymentService;
import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.reservation.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * MEMBER 5 - Panditharathne M.P. (IT25101826)
 * Rental Checkout & Return Processing Module - Controller
 * Staff-facing handover and return processing (mobile-friendly forms).
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutReturnService checkoutReturnService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final VehicleSearchService vehicleSearchService;
    private final UserRepository userRepository;

    public CheckoutController(CheckoutReturnService checkoutReturnService, ReservationService reservationService,
                              PaymentService paymentService, VehicleSearchService vehicleSearchService,
                              UserRepository userRepository) {
        this.checkoutReturnService = checkoutReturnService;
        this.reservationService = reservationService;
        this.paymentService = paymentService;
        this.vehicleSearchService = vehicleSearchService;
        this.userRepository = userRepository;
    }

    @GetMapping("/handover")
    public String handoverForm(@RequestParam(required = false) Long reservationId, Model model,
                               HttpSession session, @RequestParam(required = false) String error) {
        model.addAttribute("fuelLevels", CheckoutRecord.FuelLevel.values());
        model.addAttribute("error", error);
        List<CheckoutOperationView> eligible = eligiblePaidReservations();
        model.addAttribute("eligiblePickups", eligible);
        List<CheckoutOperationView> futurePaidPickups = new ArrayList<>();
        for (Reservation reservation : reservationService.getAllReservations()) {
            if (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED
                    && java.time.LocalDate.now().isBefore(reservation.getStartDate())
                    && checkoutReturnService.findByReservation(reservation.getId()) == null) {
                CheckoutOperationView view = operationForReservation(reservation.getId(), null);
                if (view != null && view.isPaid()) futurePaidPickups.add(view);
            }
        }
        model.addAttribute("futurePaidPickups", futurePaidPickups);
        if (reservationId != null) {
            CheckoutOperationView selected = eligible.stream()
                    .filter(item -> item.getReservation().getId().equals(reservationId)).findFirst().orElse(null);
            if (selected == null) {
                model.addAttribute("error", "This reservation is not eligible for vehicle handover. Confirm it and record successful payment first.");
            } else {
                model.addAttribute("operation", selected);
                model.addAttribute("staffName", currentUser(session).getFullName());
            }
        }
        return "checkout/checkout-form";
    }

    @PostMapping("/handover")
    public String submitHandover(@RequestParam Long reservationId,
                                  @RequestParam Long vehicleId,
                                  @RequestParam Integer odometer,
                                  @RequestParam CheckoutRecord.FuelLevel fuelLevel,
                                  @RequestParam(required = false) String photoUrl,
                                  @RequestParam(required = false) String staffId,
                                  @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedReturn,
                                  HttpSession session) {
        try {
            CheckoutRecord record = checkoutReturnService.recordHandover(
                    reservationId, vehicleId, odometer, fuelLevel, photoUrl,
                    "STAFF-" + currentUser(session).getId(), expectedReturn);
            return "redirect:/checkout/return?checkoutRecordId=" + record.getId();
        } catch (RuntimeException ex) {
            return "redirect:/checkout/handover?reservationId=" + reservationId + "&error=" + urlEncode(ex.getMessage());
        }
    }

    @GetMapping("/return")
    public String returnForm(@RequestParam Long checkoutRecordId, Model model,
                             @RequestParam(required = false) String error) {
        CheckoutRecord record = checkoutReturnService.getRecord(checkoutRecordId);
        if (record.getStage() != CheckoutRecord.RecordStage.HANDOVER) {
            return "redirect:/checkout/manage";
        }
        CheckoutOperationView operation = operationForReservation(record.getReservationId(), record);
        if (operation == null) return "redirect:/checkout/manage";
        model.addAttribute("checkoutRecordId", checkoutRecordId);
        model.addAttribute("fuelLevels", CheckoutRecord.FuelLevel.values());
        model.addAttribute("operation", operation);
        model.addAttribute("error", error);
        return "checkout/return-form";
    }

    @PostMapping("/return")
    public String submitReturn(@RequestParam Long checkoutRecordId,
                                @RequestParam Integer odometer,
                                @RequestParam CheckoutRecord.FuelLevel fuelLevel,
                                @RequestParam(required = false) String photoUrl,
                                @RequestParam(required = false) String staffId,
                                @RequestParam(required = false) String damageDescription,
                                @RequestParam(required = false) DamageReport.Severity damageSeverity,
                                HttpSession session) {
        try {
            if (damageDescription != null && !damageDescription.isBlank()) {
                if (damageSeverity == null) throw new IllegalArgumentException("Choose a damage severity when reporting damage.");
            }
            CheckoutRecord record = checkoutReturnService.recordReturn(checkoutRecordId, odometer, fuelLevel, photoUrl,
                    "STAFF-" + currentUser(session).getId());
            if (damageDescription != null && !damageDescription.isBlank()) {
                DamageReport report = new DamageReport();
                report.setDescription(damageDescription);
                report.setSeverity(damageSeverity);
                checkoutReturnService.logDamage(checkoutRecordId, report);
            }
            return "redirect:/checkout/manage?completed=" + record.getId();
        } catch (RuntimeException ex) {
            return "redirect:/checkout/return?checkoutRecordId=" + checkoutRecordId + "&error=" + urlEncode(ex.getMessage());
        }
    }

    // ---- Checkout UI CRUD ----
    @GetMapping("/manage") public String manage(Model model, @RequestParam(required = false) Long completed,
                                                  @RequestParam(required = false) String error,
                                                  @RequestParam(required = false) String success) {
        List<CheckoutOperationView> pickup = new ArrayList<>(), unpaid = new ArrayList<>(), active = new ArrayList<>(), completedList = new ArrayList<>();
        for (Reservation reservation : reservationService.getAllReservations()) {
            CheckoutRecord record = checkoutReturnService.findByReservation(reservation.getId());
            CheckoutOperationView view = operationForReservation(reservation.getId(), record);
            if (view == null) continue;
            if (record != null && record.getStage() == CheckoutRecord.RecordStage.HANDOVER) active.add(view);
            else if (record != null && record.getStage() == CheckoutRecord.RecordStage.RETURNED) completedList.add(view);
            else if (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED) {
                if (view.isPaid()) pickup.add(view); else unpaid.add(view);
            }
        }
        model.addAttribute("pickupOperations", pickup);
        model.addAttribute("unpaidOperations", unpaid);
        model.addAttribute("activeOperations", active);
        model.addAttribute("completedOperations", completedList);
        model.addAttribute("records", checkoutReturnService.getAllRecords());
        model.addAttribute("completed", completed);
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "checkout/manage";
    }
    @GetMapping("/new") public String newRecord(Model model) { model.addAttribute("record", new CheckoutRecord()); addCheckoutOptions(model); return "checkout/record-form"; }
    @PostMapping("/new") public String createRecord(@ModelAttribute("record") CheckoutRecord record, Model model, RedirectAttributes attrs) { try { checkoutReturnService.createRecord(record); attrs.addFlashAttribute("success", "Checkout record created successfully."); return "redirect:/checkout/manage"; } catch (RuntimeException ex) { model.addAttribute("error", ex.getMessage()); addCheckoutOptions(model); return "checkout/record-form"; } }
    @GetMapping("/edit/{id}") public String editRecord(@PathVariable Long id, Model model,
                                                 @RequestParam(required = false) String error) {
        CheckoutRecord record = checkoutReturnService.getRecord(id);
        if (record.getStage() == CheckoutRecord.RecordStage.RETURNED) {
            return "redirect:/checkout/manage?error=" + urlEncode("Completed return records are read-only to preserve rental history.");
        }
        model.addAttribute("record", record);
        model.addAttribute("error", error);
        addCheckoutOptions(model);
        return "checkout/record-form";
    }
    @PostMapping("/edit/{id}") public String updateRecord(@PathVariable Long id, @ModelAttribute("record") CheckoutRecord record, RedirectAttributes attrs) {
        try {
            checkoutReturnService.updateRecord(id, record);
            attrs.addFlashAttribute("success", "Handover details updated successfully.");
            return "redirect:/checkout/manage";
        } catch (RuntimeException ex) {
            attrs.addAttribute("error", ex.getMessage());
            return "redirect:/checkout/edit/" + id;
        }
    }
    @PostMapping("/delete/{id}") public String deleteRecord(@PathVariable Long id, RedirectAttributes attrs) {
        try {
            checkoutReturnService.deleteRecord(id);
            attrs.addFlashAttribute("success", "Completed checkout record and linked damage reports deleted.");
        } catch (RuntimeException ex) {
            attrs.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/checkout/manage";
    }
    private void addCheckoutOptions(Model model) { model.addAttribute("stages", CheckoutRecord.RecordStage.values()); model.addAttribute("fuelLevels", CheckoutRecord.FuelLevel.values()); }

    private List<CheckoutOperationView> eligiblePaidReservations() {
        List<CheckoutOperationView> result = new ArrayList<>();
        for (Reservation reservation : reservationService.getAllReservations()) {
            if (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED
                    && !java.time.LocalDate.now().isBefore(reservation.getStartDate())
                    && checkoutReturnService.findByReservation(reservation.getId()) == null) {
                CheckoutOperationView view = operationForReservation(reservation.getId(), null);
                if (view != null && view.isPaid()) result.add(view);
            }
        }
        return result;
    }

    private CheckoutOperationView operationForReservation(Long reservationId, CheckoutRecord record) {
        Reservation reservation = reservationService.getReservation(reservationId);
        com.rentease.renteasesystem.booking.model.Vehicle vehicle;
        try { vehicle = vehicleSearchService.getVehicleDetails(reservation.getVehicleId()); }
        catch (IllegalArgumentException ex) { return null; }
        var customer = userRepository.findById(reservation.getCustomerId()).orElse(null);
        Payment payment = paymentService.getSuccessfulPaymentForReservation(reservationId);
        var invoice = payment == null ? null : paymentService.findInvoiceByPayment(payment.getId());
        return new CheckoutOperationView(reservation, vehicle, customer, record, payment, invoice);
    }

    private SessionUser currentUser(HttpSession session) {
        return (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value == null ? "Unable to complete this operation." : value,
                java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- JSON API ----

    @RestController
    @RequestMapping("/api/checkout")
    public static class CheckoutApiController {

        private final CheckoutReturnService checkoutReturnService;

        public CheckoutApiController(CheckoutReturnService checkoutReturnService) {
            this.checkoutReturnService = checkoutReturnService;
        }

        @PostMapping("/handover")
        public ResponseEntity<CheckoutRecord> handover(@RequestBody CheckoutRecord record) {
            return ResponseEntity.ok(checkoutReturnService.recordHandover(
                    record.getReservationId(), record.getVehicleId(), record.getHandoverOdometer(),
                    record.getHandoverFuelLevel(), record.getHandoverPhotoUrl(), record.getHandoverStaffId(),
                    record.getExpectedReturnTimestamp()));
        }

        @PutMapping("/{id}/return")
        public ResponseEntity<CheckoutRecord> processReturn(@PathVariable Long id, @RequestBody CheckoutRecord payload) {
            return ResponseEntity.ok(checkoutReturnService.recordReturn(
                    id, payload.getReturnOdometer(), payload.getReturnFuelLevel(),
                    payload.getReturnPhotoUrl(), payload.getReturnStaffId()));
        }

        @PostMapping("/{id}/damage")
        public ResponseEntity<DamageReport> logDamage(@PathVariable Long id, @RequestBody DamageReport report) {
            return ResponseEntity.ok(checkoutReturnService.logDamage(id, report));
        }

        @GetMapping("/{id}/damage")
        public ResponseEntity<List<DamageReport>> getDamage(@PathVariable Long id) {
            return ResponseEntity.ok(checkoutReturnService.getDamageReports(id));
        }

        @GetMapping("/pending-returns")
        public ResponseEntity<List<CheckoutRecord>> pendingReturns() {
            return ResponseEntity.ok(checkoutReturnService.getPendingReturns());
        }
        @GetMapping("/records")
        public ResponseEntity<List<CheckoutRecord>> allRecords() { return ResponseEntity.ok(checkoutReturnService.getAllRecords()); }

        @GetMapping("/records/{id}")
        public ResponseEntity<CheckoutRecord> getRecord(@PathVariable Long id) { return ResponseEntity.ok(checkoutReturnService.getRecord(id)); }

        @PostMapping("/records")
        public ResponseEntity<CheckoutRecord> createRecord(@Valid @RequestBody CheckoutRecord record) { return ResponseEntity.status(201).body(checkoutReturnService.createRecord(record)); }

        @PutMapping("/records/{id}")
        public ResponseEntity<CheckoutRecord> updateRecord(@PathVariable Long id, @Valid @RequestBody CheckoutRecord record) { return ResponseEntity.ok(checkoutReturnService.updateRecord(id, record)); }

        @DeleteMapping("/records/{id}")
        public ResponseEntity<Void> deleteRecord(@PathVariable Long id) { checkoutReturnService.deleteRecord(id); return ResponseEntity.noContent().build(); }

        @GetMapping("/damage-reports")
        public ResponseEntity<List<DamageReport>> allDamageReports() { return ResponseEntity.ok(checkoutReturnService.getAllDamageReports()); }

        @GetMapping("/damage-reports/{id}")
        public ResponseEntity<DamageReport> getDamageReport(@PathVariable Long id) { return ResponseEntity.ok(checkoutReturnService.getDamageReport(id)); }

        @PutMapping("/damage-reports/{id}")
        public ResponseEntity<DamageReport> updateDamageReport(@PathVariable Long id, @RequestBody DamageReport report) { return ResponseEntity.ok(checkoutReturnService.updateDamageReport(id, report)); }

        @DeleteMapping("/damage-reports/{id}")
        public ResponseEntity<Void> deleteDamageReport(@PathVariable Long id) { checkoutReturnService.deleteDamageReport(id); return ResponseEntity.noContent().build(); }
    }
}
