package com.rentease.renteasesystem.checkout.service;

import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import com.rentease.renteasesystem.checkout.model.DamageReport;
import com.rentease.renteasesystem.checkout.repository.CheckoutRepository;
import com.rentease.renteasesystem.checkout.repository.DamageReportRepository;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.booking.repository.VehicleRepository;
import com.rentease.renteasesystem.payment.model.Payment;
import com.rentease.renteasesystem.payment.service.PaymentService;
import com.rentease.renteasesystem.reservation.model.Reservation;
import com.rentease.renteasesystem.reservation.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * MEMBER 5 - Panditharathne M.P. (IT25101826)
 * Rental Checkout & Return Processing Module - Business Logic
 * Records odometer/fuel/handover details, computes late-return fees, and
 * logs damage costs at return time.
 */
@Service
public class CheckoutReturnService {

    private static final BigDecimal LATE_FEE_PER_HOUR = new BigDecimal("250.00"); // LKR

    private final CheckoutRepository checkoutRepository;
    private final DamageReportRepository damageReportRepository;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final VehicleRepository vehicleRepository;

    public CheckoutReturnService(CheckoutRepository checkoutRepository, DamageReportRepository damageReportRepository,
                                 ReservationService reservationService, PaymentService paymentService,
                                 VehicleRepository vehicleRepository) {
        this.checkoutRepository = checkoutRepository;
        this.damageReportRepository = damageReportRepository;
        this.reservationService = reservationService;
        this.paymentService = paymentService;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public CheckoutRecord recordHandover(Long reservationId, Long vehicleId, Integer odometer,
                                          CheckoutRecord.FuelLevel fuelLevel, String photoUrl,
                                          String staffId, LocalDateTime expectedReturn) {
        if (reservationId == null || vehicleId == null || odometer == null || odometer < 0
                || fuelLevel == null || staffId == null || staffId.isBlank() || expectedReturn == null) {
            throw new IllegalArgumentException("Complete all required handover details with valid values.");
        }
        Reservation reservation = reservationService.getReservation(reservationId);
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked out.");
        }
        if (LocalDate.now().isBefore(reservation.getStartDate())) {
            throw new IllegalStateException("Vehicle handover cannot happen before the reservation pickup date.");
        }
        if (!reservation.getVehicleId().equals(vehicleId)) {
            throw new IllegalArgumentException("The selected vehicle does not belong to this reservation.");
        }
        if (checkoutRepository.existsByReservationId(reservationId)) {
            throw new IllegalStateException("This reservation already has a checkout record.");
        }
        Payment payment = paymentService.getSuccessfulPaymentForReservation(reservationId);
        if (payment == null || payment.getStatus() != Payment.PaymentStatus.SUCCESS
                || !reservation.getCustomerId().equals(payment.getCustomerId())) {
            throw new IllegalStateException("A successful payment is required before vehicle handover.");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + vehicleId));
        if (vehicle.getStatus() != Vehicle.VehicleStatus.AVAILABLE
                && vehicle.getStatus() != Vehicle.VehicleStatus.BOOKED) {
            throw new IllegalStateException("This vehicle is not available for handover.");
        }
        boolean alreadyOnRoad = checkoutRepository.findByVehicleId(vehicleId).stream()
                .anyMatch(existing -> existing.getStage() == CheckoutRecord.RecordStage.HANDOVER);
        if (alreadyOnRoad) throw new IllegalStateException("This vehicle is already checked out on another reservation.");
        if (expectedReturn.toLocalDate().isBefore(reservation.getEndDate())) {
            throw new IllegalArgumentException("Expected return cannot be earlier than the reservation end date.");
        }
        if (!expectedReturn.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expected return must be in the future.");
        }
        vehicle.setStatus(Vehicle.VehicleStatus.BOOKED);
        vehicleRepository.save(vehicle);

        CheckoutRecord record = new CheckoutRecord();
        record.setReservationId(reservationId);
        record.setVehicleId(vehicleId);
        record.setStage(CheckoutRecord.RecordStage.HANDOVER);
        record.setHandoverOdometer(odometer);
        record.setHandoverFuelLevel(fuelLevel);
        record.setHandoverTimestamp(LocalDateTime.now());
        record.setHandoverPhotoUrl(photoUrl);
        record.setHandoverStaffId(staffId);
        record.setExpectedReturnTimestamp(expectedReturn);
        return checkoutRepository.save(record);
    }

    @Transactional
    public CheckoutRecord recordReturn(Long checkoutRecordId, Integer odometer,
                                        CheckoutRecord.FuelLevel fuelLevel, String photoUrl, String staffId) {
        CheckoutRecord record = getRecord(checkoutRecordId);

        if (record.getStage() != CheckoutRecord.RecordStage.HANDOVER || record.getReturnTimestamp() != null) {
            throw new IllegalStateException("This rental has already been returned.");
        }
        if (odometer == null || odometer < 0 || fuelLevel == null || staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("Complete all required return details with valid values.");
        }

        if (odometer < record.getHandoverOdometer()) {
            throw new IllegalArgumentException("Return odometer reading cannot be less than the handover reading.");
        }

        record.setReturnOdometer(odometer);
        record.setReturnFuelLevel(fuelLevel);
        record.setReturnTimestamp(LocalDateTime.now());
        record.setReturnPhotoUrl(photoUrl);
        record.setReturnStaffId(staffId);
        record.setStage(CheckoutRecord.RecordStage.RETURNED);
        record.setLateFee(calculateLateFee(record.getExpectedReturnTimestamp(), record.getReturnTimestamp()));

        CheckoutRecord saved = checkoutRepository.save(record);
        reservationService.completeRental(record.getReservationId());
        Vehicle vehicle = vehicleRepository.findById(record.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + record.getVehicleId()));
        boolean otherOpenReservation = reservationService.getAllReservations().stream()
                .anyMatch(other -> !other.getId().equals(record.getReservationId())
                        && other.getVehicleId().equals(record.getVehicleId())
                        && (other.getStatus() == Reservation.ReservationStatus.PENDING
                            || other.getStatus() == Reservation.ReservationStatus.CONFIRMED));
        boolean otherActiveRental = checkoutRepository.findByVehicleId(record.getVehicleId()).stream()
                .anyMatch(other -> !other.getId().equals(record.getId())
                        && other.getStage() == CheckoutRecord.RecordStage.HANDOVER);
        if (vehicle.getStatus() == Vehicle.VehicleStatus.BOOKED && !otherOpenReservation && !otherActiveRental) {
            vehicle.setStatus(Vehicle.VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }
        return saved;
    }

    /**
     * Charges LATE_FEE_PER_HOUR for every full hour past the expected return time.
     * On-time or early returns incur no fee.
     */
    public BigDecimal calculateLateFee(LocalDateTime expected, LocalDateTime actual) {
        if (expected == null || actual == null || !actual.isAfter(expected)) {
            return BigDecimal.ZERO;
        }
        long lateHours = (long) Math.ceil(Duration.between(expected, actual).toMinutes() / 60.0);
        return LATE_FEE_PER_HOUR.multiply(BigDecimal.valueOf(lateHours)).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public DamageReport logDamage(Long checkoutRecordId, DamageReport damageReport) {
        CheckoutRecord record = getRecord(checkoutRecordId);
        damageReport.setCheckoutRecord(record);
        return damageReportRepository.save(damageReport);
    }

    public List<DamageReport> getDamageReports(Long checkoutRecordId) {
        return damageReportRepository.findByCheckoutRecord_Id(checkoutRecordId);
    }

    public List<CheckoutRecord> getAllRecords() { return checkoutRepository.findAll(); }

    @Transactional
    public CheckoutRecord createRecord(CheckoutRecord input) {
        return recordHandover(input.getReservationId(), input.getVehicleId(), input.getHandoverOdometer(),
                input.getHandoverFuelLevel(), input.getHandoverPhotoUrl(), input.getHandoverStaffId(),
                input.getExpectedReturnTimestamp());
    }

    @Transactional
    public CheckoutRecord updateRecord(Long id, CheckoutRecord input) {
        CheckoutRecord record = getRecord(id);
        if (record.getStage() == CheckoutRecord.RecordStage.RETURNED) {
            throw new IllegalStateException("A completed return record is read-only. Preserve it as rental history.");
        }
        if (input.getHandoverOdometer() == null || input.getHandoverOdometer() < 0
                || input.getHandoverFuelLevel() == null || input.getExpectedReturnTimestamp() == null) {
            throw new IllegalArgumentException("Provide a valid handover odometer, fuel level and expected return time.");
        }
        if (input.getExpectedReturnTimestamp().toLocalDate().isBefore(
                reservationService.getReservation(record.getReservationId()).getEndDate())) {
            throw new IllegalArgumentException("Expected return cannot be earlier than the reservation end date.");
        }
        record.setHandoverOdometer(input.getHandoverOdometer());
        record.setHandoverFuelLevel(input.getHandoverFuelLevel());
        record.setHandoverPhotoUrl(input.getHandoverPhotoUrl());
        record.setExpectedReturnTimestamp(input.getExpectedReturnTimestamp());
        return checkoutRepository.save(record);
    }

    @Transactional
    public void deleteRecord(Long id) {
        CheckoutRecord record = getRecord(id);
        if (record.getStage() == CheckoutRecord.RecordStage.HANDOVER) {
            throw new IllegalStateException("An active rental cannot be removed before its return is recorded.");
        }
        damageReportRepository.deleteByCheckoutRecord_Id(id);
        checkoutRepository.deleteById(id);
    }

    public DamageReport getDamageReport(Long id) {
        return damageReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Damage report not found with id: " + id));
    }

    public List<DamageReport> getAllDamageReports() { return damageReportRepository.findAll(); }

    @Transactional
    public DamageReport updateDamageReport(Long id, DamageReport input) {
        DamageReport report = getDamageReport(id);
        if (input.getCheckoutRecord() != null) report.setCheckoutRecord(getRecord(input.getCheckoutRecord().getId()));
        report.setDescription(input.getDescription());
        report.setSeverity(input.getSeverity());
        report.setEstimatedRepairCost(input.getEstimatedRepairCost());
        report.setPhotoUrl(input.getPhotoUrl());
        return damageReportRepository.save(report);
    }

    @Transactional
    public void deleteDamageReport(Long id) {
        getDamageReport(id);
        damageReportRepository.deleteById(id);
    }

    public CheckoutRecord getRecord(Long id) {
        return checkoutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Checkout record not found with id: " + id));
    }

    public CheckoutRecord getByReservation(Long reservationId) {
        return checkoutRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No checkout record for reservation: " + reservationId));
    }

    public CheckoutRecord findByReservation(Long reservationId) {
        return checkoutRepository.findByReservationId(reservationId).orElse(null);
    }

    public List<CheckoutRecord> getPendingReturns() {
        return checkoutRepository.findByStage(CheckoutRecord.RecordStage.HANDOVER);
    }
}
