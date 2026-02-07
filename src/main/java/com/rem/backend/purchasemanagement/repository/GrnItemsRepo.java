package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.grn.GrnItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrnItemsRepo extends JpaRepository<GrnItems, Long> {

    List<GrnItems> findByGrnId(Long grnId);

    List<GrnItems> findByPoItemId(Long poItemId);

    @Query("SELECT COALESCE(SUM(g.quantityReceived), 0) FROM GrnItems g WHERE g.poItemId = :poItemId")
    Double getTotalReceivedQuantityByPoItemId(@Param("poItemId") Long poItemId);
}
