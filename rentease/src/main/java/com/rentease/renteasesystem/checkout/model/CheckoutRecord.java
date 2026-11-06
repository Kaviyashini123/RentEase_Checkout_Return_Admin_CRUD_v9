package com.rentease.renteasesystem.checkout.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MEMBER 5 - Panditharathne M.P. (IT25101826)
 * Rental Checkout & Return Processing Module
 * NOTE: reservationId/vehicleId are plain Long references to other modules'
 * entities (Reservation / Booking), preserving strict module independence.
 */
@Entity
@Table(name = "checkout_records")
public class CheckoutRecord {

    public enum RecordStage { HANDOVER, RETURNED }
    public enum FuelLevel { EMPTY, QUARTER, HALF, THREE_QUARTER, FULL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    private RecordStage stage;

    // --- Handover details ---
    private Integer handoverOdometer;
    @Enumerated(EnumType.STRING)
    private FuelLevel handoverFuelLevel;
    private LocalDateTime handoverTimestamp;
    private String handoverPhotoUrl;
    private String handoverStaffId;

    // --- Return details ---
    private Integer returnOdometer;
    @Enumerated(EnumType.STRING)
    private FuelLevel returnFuelLevel;
    private LocalDateTime returnTimestamp;
    private String returnPhotoUrl;
    private String returnStaffId;

    private LocalDateTime expectedReturnTimestamp;

    @Column(precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    public CheckoutRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public RecordStage getStage() { return stage; }
    public void setStage(RecordStage stage) { this.stage = stage; }
    public Integer getHandoverOdometer() { return handoverOdometer; }
    public void setHandoverOdometer(Integer handoverOdometer) { this.handoverOdometer = handoverOdometer; }
    public FuelLevel getHandoverFuelLevel() { return handoverFuelLevel; }
    public void setHandoverFuelLevel(FuelLevel handoverFuelLevel) { this.handoverFuelLevel = handoverFuelLevel; }
    public LocalDateTime getHandoverTimestamp() { return handoverTimestamp; }
    public void setHandoverTimestamp(LocalDateTime handoverTimestamp) { this.handoverTimestamp = handoverTimestamp; }
    public String getHandoverPhotoUrl() { return handoverPhotoUrl; }
    public void setHandoverPhotoUrl(String handoverPhotoUrl) { this.handoverPhotoUrl = handoverPhotoUrl; }
    public String getHandoverStaffId() { return handoverStaffId; }
    public void setHandoverStaffId(String handoverStaffId) { this.handoverStaffId = handoverStaffId; }
    public Integer getReturnOdometer() { return returnOdometer; }
    public void setReturnOdometer(Integer returnOdometer) { this.returnOdometer = returnOdometer; }
    public FuelLevel getReturnFuelLevel() { return returnFuelLevel; }
    public void setReturnFuelLevel(FuelLevel returnFuelLevel) { this.returnFuelLevel = returnFuelLevel; }
    public LocalDateTime getReturnTimestamp() { return returnTimestamp; }
    public void setReturnTimestamp(LocalDateTime returnTimestamp) { this.returnTimestamp = returnTimestamp; }
    public String getReturnPhotoUrl() { return returnPhotoUrl; }
    public void setReturnPhotoUrl(String returnPhotoUrl) { this.returnPhotoUrl = returnPhotoUrl; }
    public String getReturnStaffId() { return returnStaffId; }
    public void setReturnStaffId(String returnStaffId) { this.returnStaffId = returnStaffId; }
    public LocalDateTime getExpectedReturnTimestamp() { return expectedReturnTimestamp; }
    public void setExpectedReturnTimestamp(LocalDateTime expectedReturnTimestamp) { this.expectedReturnTimestamp = expectedReturnTimestamp; }
    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }
}
