package com.rentease.renteasesystem.payment.repository;

import com.rentease.renteasesystem.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * MEMBER 3 - Herath H.M.S.P. (IT25101807)
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findByReservationId(Long reservationId);
    Optional<Payment> findByTransactionReference(String transactionReference);
}
