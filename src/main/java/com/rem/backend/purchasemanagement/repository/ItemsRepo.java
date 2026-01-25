package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.items.Items;
import com.rem.backend.purchasemanagement.entity.items.ItemsUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemsRepo extends JpaRepository<Items , Long> {

   Page<Items> findByOrganizationId(long OrgId , Pageable pageable);

    Optional<Items> findByOrganizationIdAndCode(long OrgId , String code);

    Optional<Items> findByCodeAndIdNot(String code, Long id);

}
