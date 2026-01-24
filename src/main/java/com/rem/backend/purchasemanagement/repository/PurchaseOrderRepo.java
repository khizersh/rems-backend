package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder , Long> {


    // Fetch the last PO by ID (or by createdDate)
    Optional<PurchaseOrder> findTopByOrderByIdDesc();

    // OR if you want by date
    Optional<PurchaseOrder> findTopByOrderByCreatedDateDesc();
}
