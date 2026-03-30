package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepo extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findAllByPoId(Long poId);
}
