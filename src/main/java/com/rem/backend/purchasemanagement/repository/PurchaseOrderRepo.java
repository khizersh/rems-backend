package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder , Long> {


    // Fetch the last PO by ID (or by createdDate)
    Optional<PurchaseOrder> findTopByOrderByIdDesc();

    List<PurchaseOrder> findByOrgId(long orgId, Pageable pageable);

    // New: find all by organization and status (no pagination)
    List<PurchaseOrder> findByOrgIdAndStatus(long orgId, PoStatus status);

    // OR if you want by date
    Optional<PurchaseOrder> findTopByOrderByCreatedDateDesc();
}
