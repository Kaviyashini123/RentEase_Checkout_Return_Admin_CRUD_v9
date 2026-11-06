package com.rentease.renteasesystem.booking.service;

import com.rentease.renteasesystem.booking.model.Favorite;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.booking.repository.FavoriteRepository;
import com.rentease.renteasesystem.booking.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * MEMBER 1 - Pravein M. (IT25101778)
 * Vehicle Search & Browsing Module - Business Logic
 */
@Service
public class VehicleSearchService {

    private final VehicleRepository vehicleRepository;
    private final FavoriteRepository favoriteRepository;

    public VehicleSearchService(VehicleRepository vehicleRepository, FavoriteRepository favoriteRepository) {
        this.vehicleRepository = vehicleRepository;
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * Filters vehicles by type, location, price range, transmission and fuel type.
     * Date-based availability (overlap checking) is intentionally delegated to the
     * Booking module (Member 2) via a list of unavailable vehicle IDs, keeping this
     * service free of any cross-module entity coupling.
     */
    public List<Vehicle> searchVehicles(Vehicle.VehicleType type,
                                         String location,
                                         Vehicle.TransmissionType transmission,
                                         Vehicle.FuelType fuelType,
                                         BigDecimal minPrice,
                                         BigDecimal maxPrice) {
        // With no filters, use the simple AVAILABLE query so the booking page
        // always shows the catalogue without relying on nullable SQL parameters.
        if (type == null && location == null && transmission == null && fuelType == null
                && minPrice == null && maxPrice == null) {
            return vehicleRepository.findByStatus(Vehicle.VehicleStatus.AVAILABLE);
        }
        return vehicleRepository.searchVehicles(type, location, transmission, fuelType, minPrice, maxPrice);
    }

    public List<Vehicle> searchAvailableExcludingBooked(List<Long> unavailableVehicleIds) {
        if (unavailableVehicleIds == null || unavailableVehicleIds.isEmpty()) {
            return vehicleRepository.findByStatus(Vehicle.VehicleStatus.AVAILABLE);
        }
        return vehicleRepository.findAvailableExcluding(unavailableVehicleIds);
    }

    public List<Vehicle> quickSearch(String keyword) {
        return vehicleRepository.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(keyword, keyword);
    }

    public Vehicle getVehicleDetails(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + id));
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicleRepository.existsByPlateNumberIgnoreCase(vehicle.getPlateNumber())) {
            throw new IllegalArgumentException("A vehicle with this plate number already exists.");
        }
        vehicle.setStatus(vehicle.getStatus() == null ? Vehicle.VehicleStatus.AVAILABLE : vehicle.getStatus());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle input) {
        Vehicle vehicle = getVehicleDetails(id);
        if (!vehicle.getPlateNumber().equalsIgnoreCase(input.getPlateNumber())
                && vehicleRepository.existsByPlateNumberIgnoreCase(input.getPlateNumber())) {
            throw new IllegalArgumentException("A vehicle with this plate number already exists.");
        }
        vehicle.setBrand(input.getBrand());
        vehicle.setModel(input.getModel());
        vehicle.setManufactureYear(input.getManufactureYear());
        vehicle.setPlateNumber(input.getPlateNumber());
        vehicle.setType(input.getType());
        vehicle.setTransmission(input.getTransmission());
        vehicle.setFuelType(input.getFuelType());
        vehicle.setSeatingCapacity(input.getSeatingCapacity());
        vehicle.setDailyRate(input.getDailyRate());
        vehicle.setLocation(input.getLocation());
        vehicle.setStatus(input.getStatus());
        vehicle.setDescription(input.getDescription());
        vehicle.setImageUrl(input.getImageUrl());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        getVehicleDetails(id);
        vehicleRepository.deleteById(id);
    }

    @Transactional
    public Favorite addFavorite(Long customerId, Long vehicleId) {
        if (favoriteRepository.existsByCustomerIdAndVehicle_Id(customerId, vehicleId)) {
            return favoriteRepository.findByCustomerIdAndVehicle_Id(customerId, vehicleId).get();
        }
        Vehicle vehicle = getVehicleDetails(vehicleId);
        return favoriteRepository.save(new Favorite(customerId, vehicle));
    }

    @Transactional
    public void removeFavorite(Long customerId, Long vehicleId) {
        favoriteRepository.deleteByCustomerIdAndVehicle_Id(customerId, vehicleId);
    }

    public List<Favorite> getFavoritesForCustomer(Long customerId) {
        return favoriteRepository.findByCustomerId(customerId);
    }
}
