package com.rentease.renteasesystem.payment.service;

import com.rentease.renteasesystem.payment.model.DiscountCode;
import com.rentease.renteasesystem.payment.model.Invoice;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.payment.repository.DiscountCodeRepository;
import com.rentease.renteasesystem.payment.repository.InvoiceRepository;
import com.rentease.renteasesystem.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

/**
 * MEMBER 3 - Herath H.M.S.P. (IT25101807)
 * Payment & Invoicing Module - Business Logic
 * Handles cost calculation (duration x rate + fees - discounts), a mock
 * payment gateway, receipt/invoice generation, and refund workflows.
 */
@Service
public class PaymentService {

    private static final BigDecimal SERVICE_FEE = new BigDecimal("500.00"); // flat fee, LKR

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final DiscountCodeRepository discountCodeRepository;

    public PaymentService(PaymentRepository paymentRepository,
                           InvoiceRepository invoiceRepository,
                           DiscountCodeRepository discountCodeRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.discountCodeRepository = discountCodeRepository;
    }

    /**
     * cost = (durationInDays x dailyRate) + serviceFee - discount
     */
    public BigDecimal calculateCost(LocalDate startDate, LocalDate endDate, BigDecimal dailyRate, String discountCode) {
        long days = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
        BigDecimal base = dailyRate.multiply(BigDecimal.valueOf(days));
        BigDecimal total = base.add(SERVICE_FEE);

        if (discountCode != null && !discountCode.isBlank()) {
            BigDecimal discount = applyDiscount(total, discountCode, LocalDate.now());
            total = total.subtract(discount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal applyDiscount(BigDecimal amount, String code, LocalDate onDate) {
        return discountCodeRepository.findByCodeIgnoreCase(code)
                .filter(d -> d.isValidOn(onDate))
                .map(d -> amount.multiply(d.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Simulates calling an external payment gateway. In production this would call
     * a real provider (Stripe/PayHere/etc). Here we simulate a >90% success rate.
     */
    @Transactional
    public Payment processPayment(Long reservationId, Long customerId, BigDecimal amount, Payment.PaymentMethod method) {
        if (getSuccessfulPaymentForReservation(reservationId) != null) {
            throw new IllegalStateException("This reservation already has a successful payment.");
        }
        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setCustomerId(customerId);
        payment.setAmount(amount);
        payment.setMethod(method);

        boolean gatewaySuccess = mockGatewayCall();

        if (gatewaySuccess) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setTransactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            payment.setPaidAt(LocalDateTime.now());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Mock gateway declined the transaction. Please retry.");
        }

        return paymentRepository.save(payment);
    }

    private boolean mockGatewayCall() {
        return Math.random() > 0.05; // ~95% success simulation
    }

    @Transactional
    public Invoice generateInvoice(Payment payment, BigDecimal baseAmount, BigDecimal feesAmount, BigDecimal discountAmount) {
        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Cannot generate an invoice for a payment that was not successful.");
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + payment.getId() + "-" + System.currentTimeMillis() % 10000);
        invoice.setPayment(payment);
        invoice.setBaseAmount(baseAmount);
        invoice.setFeesAmount(feesAmount);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotalAmount(payment.getAmount());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded.");
        }
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    public Payment getSuccessfulPaymentForReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).stream()
                .filter(payment -> payment.getStatus() == Payment.PaymentStatus.SUCCESS)
                .findFirst().orElse(null);
    }

    public Invoice findInvoiceByPayment(Long paymentId) {
        return invoiceRepository.findByPayment_Id(paymentId).orElse(null);
    }

    @Transactional
    public Payment createPayment(Payment input) {
        input.setId(null);
        if (input.getStatus() == null) input.setStatus(Payment.PaymentStatus.PENDING);
        return paymentRepository.save(input);
    }

    @Transactional
    public Payment updatePayment(Long id, Payment input) {
        Payment payment = getPayment(id);
        payment.setReservationId(input.getReservationId());
        payment.setCustomerId(input.getCustomerId());
        payment.setAmount(input.getAmount());
        payment.setMethod(input.getMethod());
        payment.setStatus(input.getStatus());
        payment.setTransactionReference(input.getTransactionReference());
        payment.setPaidAt(input.getPaidAt());
        payment.setRefundedAt(input.getRefundedAt());
        payment.setFailureReason(input.getFailureReason());
        return paymentRepository.save(payment);
    }

    @Transactional
    public void deletePayment(Long id) {
        getPayment(id);
        invoiceRepository.deleteByPayment_Id(id);
        paymentRepository.deleteById(id);
    }

    public List<DiscountCode> getAllDiscountCodes() { return discountCodeRepository.findAll(); }

    public DiscountCode getDiscountCode(Long id) {
        return discountCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found with id: " + id));
    }

    @Transactional
    public DiscountCode createDiscountCode(DiscountCode input) {
        input.setId(null);
        if (discountCodeRepository.existsByCodeIgnoreCase(input.getCode())) {
            throw new IllegalArgumentException("A discount code with this code already exists.");
        }
        return discountCodeRepository.save(input);
    }

    @Transactional
    public DiscountCode updateDiscountCode(Long id, DiscountCode input) {
        DiscountCode code = getDiscountCode(id);
        if (!code.getCode().equalsIgnoreCase(input.getCode()) && discountCodeRepository.existsByCodeIgnoreCase(input.getCode())) {
            throw new IllegalArgumentException("A discount code with this code already exists.");
        }
        code.setCode(input.getCode());
        code.setPercentage(input.getPercentage());
        code.setValidFrom(input.getValidFrom());
        code.setValidUntil(input.getValidUntil());
        code.setActive(input.isActive());
        return discountCodeRepository.save(code);
    }

    @Transactional
    public void deleteDiscountCode(Long id) {
        getDiscountCode(id);
        discountCodeRepository.deleteById(id);
    }

    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + id));
    }

    public List<Invoice> getAllInvoices() { return invoiceRepository.findAll(); }

    @Transactional
    public Invoice updateInvoice(Long id, Invoice input) {
        Invoice invoice = getInvoiceById(id);
        invoice.setInvoiceNumber(input.getInvoiceNumber());
        invoice.setBaseAmount(input.getBaseAmount());
        invoice.setFeesAmount(input.getFeesAmount());
        invoice.setDiscountAmount(input.getDiscountAmount());
        invoice.setTotalAmount(input.getTotalAmount());
        invoice.setIssuedAt(input.getIssuedAt());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteInvoice(Long id) {
        getInvoiceById(id);
        invoiceRepository.deleteById(id);
    }

    public Invoice getInvoiceByPayment(Long paymentId) {
        return invoiceRepository.findByPayment_Id(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("No invoice found for payment id: " + paymentId));
    }

    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
    }
}
