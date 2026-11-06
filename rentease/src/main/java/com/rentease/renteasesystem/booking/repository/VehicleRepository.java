package com.rentease.renteasesystem.booking.repository;

import com.rentease.renteasesystem.booking.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * MEMBER 1 - Pravein M. (IT25101778)
 */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);

    boolean existsByPlateNumberIgnoreCase(String plateNumber);

    @Query("""
        SELECT v FROM Vehicle v WHERE
        (:type IS NULL OR v.type = :type) AND
        (:location IS NULL OR LOWER(v.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND
        (:transmission IS NULL OR v.transmission = :transmission) AND
        (:fuelType IS NULL OR v.fuelType = :fuelType) AND
        (:minPrice IS NULL OR v.dailyRate >= :minPrice) AND
        (:maxPrice IS NULL OR v.dailyRate <= :maxPrice) AND
        v.status = 'AVAILABLE'
        """)
    List<Vehicle> searchVehicles(
            @Param("type") Vehicle.VehicleType type,
            @Param("location") String location,
            @Param("transmission") Vehicle.TransmissionType transmission,
            @Param("fuelType") Vehicle.FuelType fuelType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    List<Vehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(String brand, String model);

    // Vehicle IDs that are NOT overlapping-booked for the requested date range.
    // Booking module owns the Reservation table; this query stays inside the
    // Booking module's own Vehicle table and simply excludes vehicles whose ids
    // are passed in as "unavailableIds" (resolved by the booking module beforehand).
    @Query("SELECT v FROM Vehicle v WHERE v.id NOT IN :unavailableIds AND v.status = 'AVAILABLE'")
    List<Vehicle> findAvailableExcluding(@Param("unavailableIds") List<Long> unavailableIds);
}
