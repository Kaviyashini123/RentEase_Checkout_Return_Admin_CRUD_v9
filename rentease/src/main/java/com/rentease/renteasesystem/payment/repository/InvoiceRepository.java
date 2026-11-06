package com.rentease.renteasesystem.payment.repository;

import com.rentease.renteasesystem.payment.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MEMBER 3 - Herath H.M.S.P. (IT25101807)
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByPayment_Id(Long paymentId);
    void deleteByPayment_Id(Long paymentId);
}
