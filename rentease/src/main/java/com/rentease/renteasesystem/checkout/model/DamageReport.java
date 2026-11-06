package com.rentease.renteasesystem.checkout.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MEMBER 5 - Panditharathne M.P. (IT25101826)
 * Rental Checkout & Return Processing Module
 */
@Entity
@Table(name = "damage_reports")
public class DamageReport {

    public enum Severity { MINOR, MODERATE, SEVERE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_record_id", nullable = false)
    private CheckoutRecord checkoutRecord;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedRepairCost;

    private String photoUrl;

    private LocalDateTime reportedAt = LocalDateTime.now();

    public DamageReport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CheckoutRecord getCheckoutRecord() { return checkoutRecord; }
    public void setCheckoutRecord(CheckoutRecord checkoutRecord) { this.checkoutRecord = checkoutRecord; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public BigDecimal getEstimatedRepairCost() { return estimatedRepairCost; }
    public void setEstimatedRepairCost(BigDecimal estimatedRepairCost) { this.estimatedRepairCost = estimatedRepairCost; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
}
