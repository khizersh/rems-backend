package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.items.Items;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemsRepo extends JpaRepository<Items , Long> {
}
