package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoice;
import com.rem.backend.purchasemanagement.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorInvoiceRepo extends JpaRepository<VendorInvoice, Long> {

    List<VendorInvoice> findByPoId(Long poId);

    List<VendorInvoice> findByGrnId(Long grnId);

    List<VendorInvoice> findByVendorId(Long vendorId);

    Page<VendorInvoice> findByOrgId(Long orgId, Pageable pageable);

    Page<VendorInvoice> findByVendorId(Long vendorId, Pageable pageable);

    Page<VendorInvoice> findByOrgIdAndStatus(Long orgId, InvoiceStatus status, Pageable pageable);

    Optional<VendorInvoice> findTopByOrderByIdDesc();

    @Query("SELECT COALESCE(SUM(v.totalAmount - v.paidAmount), 0) FROM VendorInvoice v WHERE v.vendorId = :vendorId AND v.status != 'PAID'")
    Double getPendingAmountByVendorId(@Param("vendorId") Long vendorId);
}
