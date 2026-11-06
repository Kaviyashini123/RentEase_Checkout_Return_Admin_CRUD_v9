package com.rentease.renteasesystem.reports.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * MEMBER 6 - Kaviyashini V (IT25101787)
 * Reports & Reviews Management Module
 * NOTE: customerId/vehicleId/reservationId are plain Long references to
 * keep this module decoupled from Search/Booking entities.
 */
@Entity
@Table(name = "reviews")
public class Review {

    public enum ManagementStatus { OPEN, IN_REVIEW, ACTION_REQUIRED, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @Positive(message = "Customer ID must be positive")
    @Column(nullable = false)
    private Long customerId;

    @NotNull @Positive(message = "Vehicle ID must be positive")
    @Column(nullable = false)
    private Long vehicleId;

    private Long reservationId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(nullable = false)
    private int rating; // 1-5

    @Column(length = 2000)
    private String comment;

    private LocalDateTime submittedAt = LocalDateTime.now();

    private boolean flagged = false;

    /** Nullable for backward-compatible addition to databases with existing reviews. */
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ManagementStatus managementStatus = ManagementStatus.OPEN;

    @Column(length = 2000)
    private String managementNote;

    private LocalDateTime managementUpdatedAt;

    public Review() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
    public ManagementStatus getManagementStatus() {
        return managementStatus == null ? ManagementStatus.OPEN : managementStatus;
    }
    public void setManagementStatus(ManagementStatus managementStatus) { this.managementStatus = managementStatus; }
    public String getManagementNote() { return managementNote; }
    public void setManagementNote(String managementNote) { this.managementNote = managementNote; }
    public LocalDateTime getManagementUpdatedAt() { return managementUpdatedAt; }
    public void setManagementUpdatedAt(LocalDateTime managementUpdatedAt) { this.managementUpdatedAt = managementUpdatedAt; }
}
