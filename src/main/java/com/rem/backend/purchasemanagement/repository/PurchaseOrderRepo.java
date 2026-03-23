package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.dto.PoBasicDTO;
import com.rem.backend.purchasemanagement.entity.purchaseorder.PurchaseOrder;
import com.rem.backend.purchasemanagement.enums.PoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder , Long> {


    // Fetch the last PO by ID (or by createdDate)
    Optional<PurchaseOrder> findTopByOrderByIdDesc();

    // Return a Page so controller/service can return pageable metadata
    Page<PurchaseOrder> findByOrgId(long orgId, Pageable pageable);

    // New: find all by organization and status (no pagination)
    List<PurchaseOrder> findByOrgIdAndStatus(long orgId, PoStatus status);

    // OR if you want by date
    Optional<PurchaseOrder> findTopByOrderByCreatedDateDesc();

    // New: lightweight projection returning only id and poNumber ordered newest-first (createdDate desc)
    @Query("SELECT new com.rem.backend.purchasemanagement.dto.PoBasicDTO(p.id, p.poNumber) FROM PurchaseOrder p WHERE p.orgId = :orgId ORDER BY p.createdDate DESC")
    List<PoBasicDTO> findBasicByOrgIdOrderByCreatedDateDesc(@Param("orgId") long orgId);
}
