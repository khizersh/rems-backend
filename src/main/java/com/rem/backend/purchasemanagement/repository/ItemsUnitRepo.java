package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.items.ItemsUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemsUnitRepo extends JpaRepository<ItemsUnit , Long> {
    Page<ItemsUnit> findAllByOrganizationId(long orgId , Pageable pageable);
    List<ItemsUnit> findAllByOrganizationId(long orgId );
}
