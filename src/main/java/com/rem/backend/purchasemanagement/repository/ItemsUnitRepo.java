package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.items.ItemsUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemsUnitRepo extends JpaRepository<ItemsUnit , Long> {
}
