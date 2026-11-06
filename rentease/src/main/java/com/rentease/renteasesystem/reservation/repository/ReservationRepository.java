package com.rentease.renteasesystem.reservation.repository;

import com.rentease.renteasesystem.reservation.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * MEMBER 2 - Dilmani J.A.S. (IT25101845)
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByCustomerId(Long customerId);

    List<Reservation> findByVehicleId(Long vehicleId);

    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    /**
     * Overlap-check query: two date ranges [start1,end1] and [start2,end2] overlap
     * when start1 <= end2 AND end1 >= start2. Only PENDING or CONFIRMED
     * reservations count as blocking; CANCELLED ones are ignored.
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.vehicleId = :vehicleId
        AND r.status IN ('PENDING', 'CONFIRMED')
        AND r.startDate <= :endDate
        AND r.endDate >= :startDate
        """)
    List<Reservation> findOverlappingReservations(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT r.vehicleId FROM Reservation r WHERE r.status IN ('PENDING','CONFIRMED') " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<Long> findBookedVehicleIdsInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
