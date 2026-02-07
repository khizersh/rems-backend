package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.vendorpayment.VendorPaymentPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPaymentPORepo extends JpaRepository<VendorPaymentPO, Long> {

    List<VendorPaymentPO> findByInvoiceId(Long invoiceId);

    List<VendorPaymentPO> findByVendorId(Long vendorId);

    Page<VendorPaymentPO> findByOrgId(Long orgId, Pageable pageable);

    Page<VendorPaymentPO> findByVendorId(Long vendorId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.amount), 0) FROM VendorPaymentPO v WHERE v.invoiceId = :invoiceId")
    Double getTotalPaidAmountByInvoiceId(@Param("invoiceId") Long invoiceId);
}
