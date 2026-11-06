package com.rentease.renteasesystem.reports.controller;

import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.reports.model.Review;
import com.rentease.renteasesystem.reservation.model.Reservation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReportReviewControllerTest {

    @Test
    void resolvesDirectVehicleWhenReviewHasNoReservation() {
        Review review = review(2L, 10L, null);
        Vehicle vehicle = vehicle(10L);

        assertSame(vehicle, ReportReviewController.resolveReviewVehicle(review, null, Map.of(10L, vehicle)));
    }

    @Test
    void usesCustomerMatchedReservationVehicleAsAuthoritative() {
        Review review = review(2L, 2L, 24L);
        Reservation reservation = reservation(24L, 2L, 5L);
        Vehicle reservedVehicle = vehicle(5L);
        Vehicle reviewVehicle = vehicle(2L);

        assertSame(reservedVehicle, ReportReviewController.resolveReviewVehicle(
                review, reservation, Map.of(2L, reviewVehicle, 5L, reservedVehicle)));
    }

    @Test
    void returnsUnavailableWhenReferencedVehicleDoesNotExist() {
        Review review = review(2L, 999L, null);

        assertNull(ReportReviewController.resolveReviewVehicle(review, null, Map.of()));
    }

    @Test
    void ignoresMismatchedReservationAndUsesReviewVehicleReference() {
        Review review = review(2L, 10L, 24L);
        Reservation anotherCustomersReservation = reservation(24L, 9L, 5L);
        Vehicle directlyReferencedVehicle = vehicle(10L);
        Vehicle foreignReservationVehicle = vehicle(5L);

        assertSame(directlyReferencedVehicle, ReportReviewController.resolveReviewVehicle(
                review, anotherCustomersReservation, Map.of(5L, foreignReservationVehicle, 10L, directlyReferencedVehicle)));
        assertFalse(ReportReviewController.isReservationOwnedByReviewCustomer(
                review, anotherCustomersReservation));
    }

    private static Review review(Long customerId, Long vehicleId, Long reservationId) {
        Review review = new Review();
        review.setCustomerId(customerId);
        review.setVehicleId(vehicleId);
        review.setReservationId(reservationId);
        return review;
    }

    private static Reservation reservation(Long id, Long customerId, Long vehicleId) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setCustomerId(customerId);
        reservation.setVehicleId(vehicleId);
        return reservation;
    }

    private static Vehicle vehicle(Long id) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        return vehicle;
    }
}
