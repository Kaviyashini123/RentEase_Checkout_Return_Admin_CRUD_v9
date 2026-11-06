package com.rentease.renteasesystem.payment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MEMBER 3 - Herath H.M.S.P. (IT25101807)
 * Payment & Invoicing Module
 */
@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Discount code is required")
    @Column(unique = true, nullable = false)
    private String code;

    /** Percentage discount, e.g. 10.00 means 10% off */
    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.00", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    private LocalDate validFrom;
    private LocalDate validUntil;
    private boolean active = true;

    public DiscountCode() {}

    public boolean isValidOn(LocalDate date) {
        return active
                && (validFrom == null || !date.isBefore(validFrom))
                && (validUntil == null || !date.isAfter(validUntil));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
