package com.rentease.renteasesystem.payment.controller;

import com.rentease.renteasesystem.auth.model.SessionUser;
import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.auth.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.rentease.renteasesystem.payment.model.Invoice;
import com.rentease.renteasesystem.payment.model.DiscountCode;
import jakarta.validation.Valid;
import java.util.List;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.payment.service.PaymentService;
import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.reservation.service.ReservationService;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.booking.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MEMBER 3 - Herath H.M.S.P. (IT25101807)
 * Payment & Invoicing Module - Controller
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReservationService reservationService;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService, ReservationService reservationService,
                             VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "payment/dashboard";
    }

    @GetMapping("/checkout")
    public String checkoutPage(@RequestParam Long reservationId, Model model, HttpSession session) {
        model.addAttribute("alreadyPaid", false);
        model.addAttribute("blockedMessage", null);
        Reservation reservation = ownedReservation(reservationId, session);
        Vehicle vehicle = getVehicle(reservation.getVehicleId());
        if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
            model.addAttribute("blockedMessage", "Payment is available after your reservation is confirmed.");
        } else if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            model.addAttribute("blockedMessage", "This reservation has been cancelled.");
        } else {
            Payment existing = paymentService.getSuccessfulPaymentForReservation(reservationId);
            if (existing != null) {
                Invoice invoice = paymentService.findInvoiceByPayment(existing.getId());
                if (invoice != null) return "redirect:/payment/invoice/" + invoice.getId();
                model.addAttribute("alreadyPaid", true);
            }
        }
        addCheckoutModel(model, reservation, vehicle, session);
        return "payment/checkout";
    }

    @PostMapping("/checkout")
    public String submitPayment(@RequestParam Long reservationId,
                                 @RequestParam Payment.PaymentMethod method,
                                 Model model,
                                 HttpSession session) {
        model.addAttribute("alreadyPaid", false);
        model.addAttribute("blockedMessage", null);
        Reservation reservation = ownedReservation(reservationId, session);
        Vehicle vehicle = getVehicle(reservation.getVehicleId());
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED
                && reservation.getStatus() != Reservation.ReservationStatus.COMPLETED) {
            addCheckoutModel(model, reservation, vehicle, session);
            model.addAttribute("blockedMessage", reservation.getStatus() == Reservation.ReservationStatus.PENDING
                    ? "Payment is available after your reservation is confirmed."
                    : "This reservation has been cancelled.");
            return "payment/checkout";
        }
        Payment previous = paymentService.getSuccessfulPaymentForReservation(reservationId);
        if (previous != null) {
            Invoice invoice = paymentService.findInvoiceByPayment(previous.getId());
            if (invoice != null) return "redirect:/payment/invoice/" + invoice.getId();
            model.addAttribute("alreadyPaid", true);
            addCheckoutModel(model, reservation, vehicle, session);
            return "payment/checkout";
        }
        if (method != Payment.PaymentMethod.CREDIT_CARD && method != Payment.PaymentMethod.DEBIT_CARD) {
            addCheckoutModel(model, reservation, vehicle, session);
            model.addAttribute("error", "Choose credit card or debit card to continue.");
            return "payment/checkout";
        }
        BigDecimal amount = paymentTotal(reservation, vehicle);
        Payment payment = paymentService.processPayment(reservationId, reservation.getCustomerId(), amount, method);
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            BigDecimal baseAmount = vehicle.getDailyRate().multiply(BigDecimal.valueOf(
                    Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate()))));
            Invoice invoice = paymentService.generateInvoice(payment, baseAmount, amount.subtract(baseAmount), BigDecimal.ZERO);
            return "redirect:/payment/invoice/" + invoice.getId();
        }
        model.addAttribute("error", payment.getFailureReason());
        addCheckoutModel(model, reservation, vehicle, session);
        return "payment/checkout";
    }

    @GetMapping("/invoice/{invoiceId}")
    public String viewInvoice(@PathVariable Long invoiceId, Model model, HttpSession session) {
        Invoice invoice = paymentService.getInvoiceById(invoiceId);
        SessionUser current = currentUser(session);
        if (!current.isAdmin() && !current.getId().equals(invoice.getPayment().getCustomerId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
        Reservation reservation = null;
        Vehicle vehicle = null;
        try {
            reservation = reservationService.getReservation(invoice.getPayment().getReservationId());
            vehicle = getVehicle(reservation.getVehicleId());
        } catch (IllegalArgumentException ignored) {
            // Retain access to older invoices whose reservation/vehicle row was
            // removed or never seeded; the invoice still renders its payment data.
        }
        User customer = userRepository.findById(invoice.getPayment().getCustomerId()).orElse(null);
        model.addAttribute("invoice", invoice);
        model.addAttribute("reservation", reservation);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("customer", customer);
        return "payment/invoice";
    }

    private void addCheckoutModel(Model model, Reservation reservation, Vehicle vehicle, HttpSession session) {
        BigDecimal total = paymentTotal(reservation, vehicle);
        BigDecimal base = vehicle.getDailyRate().multiply(BigDecimal.valueOf(
                Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate()))));
        User customer = userRepository.findById(reservation.getCustomerId()).orElse(null);
        model.addAttribute("reservation", reservation);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("customer", customer);
        model.addAttribute("reservationId", reservation.getId());
        model.addAttribute("amount", total);
        model.addAttribute("baseAmount", base);
        model.addAttribute("feesAmount", total.subtract(base));
        model.addAttribute("methods", List.of(Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentMethod.DEBIT_CARD));
    }

    private BigDecimal paymentTotal(Reservation reservation, Vehicle vehicle) {
        return paymentService.calculateCost(reservation.getStartDate(), reservation.getEndDate(), vehicle.getDailyRate(), null);
    }

    private Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
    }

    private Reservation ownedReservation(Long reservationId, HttpSession session) {
        Reservation reservation = reservationService.getReservation(reservationId);
        SessionUser current = currentUser(session);
        if (!current.isAdmin() && !current.getId().equals(reservation.getCustomerId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "You cannot access payment for another customer's reservation.");
        }
        return reservation;
    }

    // ---- Payment UI CRUD ----
    @GetMapping("/manage")
    public String manage(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        java.util.Map<Long, Long> invoiceIdsByPayment = new java.util.LinkedHashMap<>();
        for (Payment payment : payments) {
            Invoice invoice = paymentService.findInvoiceByPayment(payment.getId());
            if (invoice != null) invoiceIdsByPayment.put(payment.getId(), invoice.getId());
        }
        model.addAttribute("payments", payments);
        model.addAttribute("invoiceIdsByPayment", invoiceIdsByPayment);
        return "payment/manage";
    }
    @GetMapping("/new") public String newPayment(Model model) { model.addAttribute("payment", new Payment()); addPaymentOptions(model); return "payment/payment-form"; }
    @PostMapping("/new") public String createPayment(@ModelAttribute Payment payment, Model model, RedirectAttributes attrs) { try { paymentService.createPayment(payment); attrs.addFlashAttribute("success", "Payment created successfully."); return "redirect:/payment/manage"; } catch (RuntimeException ex) { model.addAttribute("error", ex.getMessage()); addPaymentOptions(model); return "payment/payment-form"; } }
    @GetMapping("/edit/{id}") public String editPayment(@PathVariable Long id, Model model) { model.addAttribute("payment", paymentService.getPayment(id)); addPaymentOptions(model); return "payment/payment-form"; }
    @PostMapping("/edit/{id}") public String updatePayment(@PathVariable Long id, @ModelAttribute Payment payment, RedirectAttributes attrs) { paymentService.updatePayment(id, payment); attrs.addFlashAttribute("success", "Payment updated successfully."); return "redirect:/payment/manage"; }
    @PostMapping("/delete/{id}") public String deletePayment(@PathVariable Long id, RedirectAttributes attrs) { paymentService.deletePayment(id); attrs.addFlashAttribute("success", "Payment deleted successfully."); return "redirect:/payment/manage"; }
    private void addPaymentOptions(Model model) { model.addAttribute("methods", Payment.PaymentMethod.values()); model.addAttribute("statuses", Payment.PaymentStatus.values()); }

    // ---- JSON API ----

    @RestController
    @RequestMapping("/api/payment")
    public static class PaymentApiController {

        private final PaymentService paymentService;
        private final ReservationService reservationService;
        private final VehicleRepository vehicleRepository;

        public PaymentApiController(PaymentService paymentService, ReservationService reservationService,
                                    VehicleRepository vehicleRepository) {
            this.paymentService = paymentService;
            this.reservationService = reservationService;
            this.vehicleRepository = vehicleRepository;
        }

        @GetMapping("/calculate")
        public ResponseEntity<BigDecimal> calculate(@RequestParam LocalDate startDate,
                                                      @RequestParam LocalDate endDate,
                                                      @RequestParam BigDecimal dailyRate,
                                                      @RequestParam(required = false) String discountCode) {
            return ResponseEntity.ok(paymentService.calculateCost(startDate, endDate, dailyRate, discountCode));
        }

        @PostMapping("/process")
        public ResponseEntity<Payment> process(@RequestParam Long reservationId,
                                                @RequestParam Long customerId,
                                                @RequestParam BigDecimal amount,
                                                @RequestParam Payment.PaymentMethod method,
                                                HttpSession session) {
            SessionUser current = currentUser(session);
            Reservation reservation = reservationService.getReservation(reservationId);
            if (!current.isAdmin() && !current.getId().equals(reservation.getCustomerId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            }
            if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED
                    && reservation.getStatus() != Reservation.ReservationStatus.COMPLETED) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                        "Payment is available only for confirmed or completed reservations.");
            }
            Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
            BigDecimal trustedAmount = paymentService.calculateCost(reservation.getStartDate(), reservation.getEndDate(), vehicle.getDailyRate(), null);
            return ResponseEntity.ok(paymentService.processPayment(reservationId, reservation.getCustomerId(), trustedAmount, method));
        }

        @PostMapping("/{paymentId}/refund")
        public ResponseEntity<Payment> refund(@PathVariable Long paymentId, HttpSession session) {
            if (!currentUser(session).isAdmin()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            return ResponseEntity.ok(paymentService.refundPayment(paymentId));
        }

        @GetMapping("/{paymentId}")
        public ResponseEntity<Payment> get(@PathVariable Long paymentId, HttpSession session) {
            Payment payment = paymentService.getPayment(paymentId);
            SessionUser current = currentUser(session);
            if (!current.isAdmin() && !current.getId().equals(payment.getCustomerId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(payment);
        }

        @GetMapping("/invoice/by-payment/{paymentId}")
        public ResponseEntity<Invoice> getInvoice(@PathVariable Long paymentId, HttpSession session) {
            Payment payment = paymentService.getPayment(paymentId);
            SessionUser current = currentUser(session);
            if (!current.isAdmin() && !current.getId().equals(payment.getCustomerId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(paymentService.getInvoiceByPayment(paymentId));
        }

        // Payment module CRUD
        @PostMapping("/payments")
        public ResponseEntity<Payment> createPayment(@Valid @RequestBody Payment payment) {
            return ResponseEntity.status(201).body(paymentService.createPayment(payment));
        }

        @GetMapping("/payments")
        public ResponseEntity<List<Payment>> allPayments() {
            return ResponseEntity.ok(paymentService.getAllPayments());
        }

        @GetMapping("/payments/{id}")
        public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
            return ResponseEntity.ok(paymentService.getPayment(id));
        }

        @PutMapping("/payments/{id}")
        public ResponseEntity<Payment> updatePayment(@PathVariable Long id, @Valid @RequestBody Payment payment) {
            return ResponseEntity.ok(paymentService.updatePayment(id, payment));
        }

        @DeleteMapping("/payments/{id}")
        public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
            paymentService.deletePayment(id);
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/discount-codes")
        public ResponseEntity<DiscountCode> createDiscountCode(@Valid @RequestBody DiscountCode code) {
            return ResponseEntity.status(201).body(paymentService.createDiscountCode(code));
        }

        @GetMapping("/discount-codes")
        public ResponseEntity<List<DiscountCode>> allDiscountCodes() {
            return ResponseEntity.ok(paymentService.getAllDiscountCodes());
        }

        @GetMapping("/discount-codes/{id}")
        public ResponseEntity<DiscountCode> getDiscountCode(@PathVariable Long id) {
            return ResponseEntity.ok(paymentService.getDiscountCode(id));
        }

        @PutMapping("/discount-codes/{id}")
        public ResponseEntity<DiscountCode> updateDiscountCode(@PathVariable Long id, @Valid @RequestBody DiscountCode code) {
            return ResponseEntity.ok(paymentService.updateDiscountCode(id, code));
        }

        @DeleteMapping("/discount-codes/{id}")
        public ResponseEntity<Void> deleteDiscountCode(@PathVariable Long id) {
            paymentService.deleteDiscountCode(id);
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/invoices")
        public ResponseEntity<List<Invoice>> allInvoices() { return ResponseEntity.ok(paymentService.getAllInvoices()); }

        @GetMapping("/invoices/{id}")
        public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) { return ResponseEntity.ok(paymentService.getInvoiceById(id)); }

        @PutMapping("/invoices/{id}")
        public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoice) { return ResponseEntity.ok(paymentService.updateInvoice(id, invoice)); }

        @DeleteMapping("/invoices/{id}")
        public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) { paymentService.deleteInvoice(id); return ResponseEntity.noContent().build(); }
    }

    private static SessionUser currentUser(HttpSession session) {
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (current == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return current;
    }

}
