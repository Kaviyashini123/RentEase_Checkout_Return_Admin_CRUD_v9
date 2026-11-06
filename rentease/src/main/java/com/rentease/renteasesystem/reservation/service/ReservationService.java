package com.rentease.renteasesystem.reservation.service;

import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * MEMBER 2 - Dilmani J.A.S. (IT25101845)
 * Booking & Reservation Management Module - Business Logic
 * Owns real-time calendar availability checks and prevents overlapping double bookings.
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Returns true if the vehicle is free for the entire requested date range.
     */
    public boolean isVehicleAvailable(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findOverlappingReservations(vehicleId, startDate, endDate).isEmpty();
    }

    @Transactional
    public Reservation createReservation(Reservation reservation) {
        validateDateRange(reservation.getStartDate(), reservation.getEndDate());

        if (!isVehicleAvailable(reservation.getVehicleId(), reservation.getStartDate(), reservation.getEndDate())) {
            throw new IllegalStateException(
                "Vehicle " + reservation.getVehicleId() + " is already booked for the selected date range.");
        }

        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation updateReservationDates(Long reservationId, LocalDate newStart, LocalDate newEnd) {
        Reservation reservation = getReservation(reservationId);
        validateDateRange(newStart, newEnd);

        boolean conflict = reservationRepository.findOverlappingReservations(reservation.getVehicleId(), newStart, newEnd)
                .stream()
                .anyMatch(r -> !r.getId().equals(reservationId));
        if (conflict) {
            throw new IllegalStateException("New dates conflict with an existing reservation for this vehicle.");
        }

        reservation.setStartDate(newStart);
        reservation.setEndDate(newEnd);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation reassignVehicle(Long reservationId, Long newVehicleId) {
        Reservation reservation = getReservation(reservationId);
        if (!isVehicleAvailable(newVehicleId, reservation.getStartDate(), reservation.getEndDate())) {
            throw new IllegalStateException("Replacement vehicle is not available for these dates.");
        }
        reservation.setVehicleId(newVehicleId);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, String reason) {
        Reservation reservation = getReservation(reservationId);
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation updateReservation(Long reservationId, Reservation input) {
        Reservation reservation = getReservation(reservationId);
        validateDateRange(input.getStartDate(), input.getEndDate());
        boolean conflict = reservationRepository.findOverlappingReservations(input.getVehicleId(), input.getStartDate(), input.getEndDate())
                .stream().anyMatch(r -> !r.getId().equals(reservationId));
        if (conflict) {
            throw new IllegalStateException("Vehicle is already booked for the selected date range.");
        }
        reservation.setCustomerId(input.getCustomerId());
        reservation.setVehicleId(input.getVehicleId());
        reservation.setStartDate(input.getStartDate());
        reservation.setEndDate(input.getEndDate());
        reservation.setEstimatedCost(input.getEstimatedCost());
        reservation.setPickupLocation(input.getPickupLocation());
        reservation.setDropoffLocation(input.getDropoffLocation());
        if (input.getStatus() != null) reservation.setStatus(input.getStatus());
        reservation.setCancellationReason(input.getCancellationReason());
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void deleteReservation(Long reservationId) {
        getReservation(reservationId);
        reservationRepository.deleteById(reservationId);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with id: " + id));
    }

    /** Marks a paid, confirmed rental complete after staff records its return. */
    @Transactional
    public Reservation completeRental(Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed reservation can be completed after return.");
        }
        reservation.setStatus(Reservation.ReservationStatus.COMPLETED);
        return reservationRepository.save(reservation);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsForCustomer(Long customerId) {
        return reservationRepository.findByCustomerId(customerId);
    }

    public List<Long> getBookedVehicleIds(LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findBookedVehicleIdsInRange(startDate, endDate);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid date range: end date must be on/after start date.");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
    }
}
