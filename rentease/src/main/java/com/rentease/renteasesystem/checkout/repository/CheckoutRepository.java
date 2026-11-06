package com.rentease.renteasesystem.checkout.repository;

import com.rentease.renteasesystem.checkout.model.CheckoutRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * MEMBER 5 - Panditharathne M.P. (IT25101826)
 */
public interface CheckoutRepository extends JpaRepository<CheckoutRecord, Long> {
    Optional<CheckoutRecord> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
    List<CheckoutRecord> findByStage(CheckoutRecord.RecordStage stage);
    List<CheckoutRecord> findByVehicleId(Long vehicleId);
}
