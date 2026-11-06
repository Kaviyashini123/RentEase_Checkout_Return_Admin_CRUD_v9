package com.rentease.renteasesystem.booking.repository;

import com.rentease.renteasesystem.booking.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * MEMBER 1 - Pravein M. (IT25101778)
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByCustomerId(Long customerId);

    Optional<Favorite> findByCustomerIdAndVehicle_Id(Long customerId, Long vehicleId);

    void deleteByCustomerIdAndVehicle_Id(Long customerId, Long vehicleId);

    boolean existsByCustomerIdAndVehicle_Id(Long customerId, Long vehicleId);
}
