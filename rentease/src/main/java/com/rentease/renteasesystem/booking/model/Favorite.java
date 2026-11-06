package com.rentease.renteasesystem.booking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MEMBER 1 - Pravein M. (IT25101778)
 * Stores a customer's favorited/saved vehicles.
 * NOTE: customerId is kept as a plain Long (no direct FK to a User entity)
 * so this module stays fully decoupled from other members' modules.
 */
@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"customerId", "vehicle_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    private LocalDateTime savedAt = LocalDateTime.now();

    public Favorite() {}

    public Favorite(Long customerId, Vehicle vehicle) {
        this.customerId = customerId;
        this.vehicle = vehicle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
