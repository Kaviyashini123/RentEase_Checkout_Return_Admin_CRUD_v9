package com.rentease.renteasesystem.reports.repository;

import com.rentease.renteasesystem.reports.model.Review;
import com.rentease.renteasesystem.reservation.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * MEMBER 6 - Kaviyashini V (IT25101787)
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByVehicleId(Long vehicleId);

    List<Review> findByCustomerId(Long customerId);

    List<Review> findByReservationId(Long reservationId);

    boolean existsByReservationId(Long reservationId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.vehicleId = :vehicleId")
    Double findAverageRatingForVehicle(Long vehicleId);

    @Query("SELECT r.vehicleId, COUNT(r) as cnt FROM Review r GROUP BY r.vehicleId ORDER BY cnt DESC")
    List<Object[]> findMostReviewedVehicles();

    /**
     * Rating summaries for real completed rental journeys. Review keeps scalar IDs,
     * so the explicit JPQL predicates form the relationship joins without changing schema.
     */
    @Query("SELECT v.id, v.brand, v.model, v.type, AVG(review.rating), COUNT(review), MIN(review.rating), MAX(review.rating) " +
            "FROM Review review, Reservation reservation, Vehicle v " +
            "WHERE review.reservationId = reservation.id " +
            "AND reservation.vehicleId = v.id AND review.customerId = reservation.customerId " +
            "AND reservation.status = :completedStatus " +
            "GROUP BY v.id, v.brand, v.model, v.type " +
            "ORDER BY AVG(review.rating) DESC, COUNT(review) DESC")
    List<Object[]> findCompletedRentalVehicleRatingSummaries(
            @Param("completedStatus") Reservation.ReservationStatus completedStatus);

    /** Genuine HAVING query for vehicles whose completed-rental average needs attention. */
    @Query("SELECT v.id " +
            "FROM Review review, Reservation reservation, Vehicle v " +
            "WHERE review.reservationId = reservation.id " +
            "AND reservation.vehicleId = v.id AND review.customerId = reservation.customerId " +
            "AND reservation.status = :completedStatus " +
            "GROUP BY v.id " +
            "HAVING AVG(review.rating) <= :threshold")
    List<Long> findCompletedRentalVehicleIdsNeedingAttention(
            @Param("completedStatus") Reservation.ReservationStatus completedStatus,
            @Param("threshold") double threshold);
}
