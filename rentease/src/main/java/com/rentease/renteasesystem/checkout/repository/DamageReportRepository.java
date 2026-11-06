package com.rentease.renteasesystem.checkout.repository;

import com.rentease.renteasesystem.checkout.model.DamageReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MEMBER 5 - Panditharathne M.P. (IT25101826)
 */
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {
    List<DamageReport> findByCheckoutRecord_Id(Long checkoutRecordId);
    void deleteByCheckoutRecord_Id(Long checkoutRecordId);
}
