package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.vendorinvoice.VendorInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorInvoiceItemRepo extends JpaRepository<VendorInvoiceItem, Long> {

    List<VendorInvoiceItem> findByInvoiceId(Long invoiceId);

    List<VendorInvoiceItem> findByGrnItemId(Long grnItemId);

    @Query("SELECT COALESCE(SUM(v.quantity), 0) FROM VendorInvoiceItem v WHERE v.grnItemId = :grnItemId")
    Double getTotalInvoicedQuantityByGrnItemId(@Param("grnItemId") Long grnItemId);
}
