package com.rentease.renteasesystem.payment.repository;

import com.rentease.renteasesystem.payment.model.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MEMBER 3 - Herath H.M.S.P. (IT25101807)
 * (Supporting repository for DiscountCode; not explicitly listed in the brief
 *  but required to back the discount lookup used by PaymentService.)
 */
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
