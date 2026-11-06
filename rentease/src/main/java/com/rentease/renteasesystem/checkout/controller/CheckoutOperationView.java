package com.rentease.renteasesystem.checkout.controller;

import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import com.rentease.renteasesystem.payment.model.Invoice;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.reservation.model.Reservation;
import java.time.LocalDate;

/** Read-only joined view of existing module records for the staff operations screen. */
public class CheckoutOperationView {
    private final Reservation reservation;
    private final Vehicle vehicle;
    private final User customer;
    private final CheckoutRecord checkout;
    private final Payment payment;
    private final Invoice invoice;

    public CheckoutOperationView(Reservation reservation, Vehicle vehicle, User customer,
                                 CheckoutRecord checkout, Payment payment, Invoice invoice) {
        this.reservation = reservation;
        this.vehicle = vehicle;
        this.customer = customer;
        this.checkout = checkout;
        this.payment = payment;
        this.invoice = invoice;
    }

    public Reservation getReservation() { return reservation; }
    public Vehicle getVehicle() { return vehicle; }
    public User getCustomer() { return customer; }
    public CheckoutRecord getCheckout() { return checkout; }
    public Payment getPayment() { return payment; }
    public Invoice getInvoice() { return invoice; }
    public boolean isPaid() { return payment != null && payment.getStatus() == Payment.PaymentStatus.SUCCESS; }
    public boolean isPickupStarted() { return !LocalDate.now().isBefore(reservation.getStartDate()); }
}
