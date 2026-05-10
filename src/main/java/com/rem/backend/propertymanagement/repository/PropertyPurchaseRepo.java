package com.rem.backend.propertymanagement.repository;

import com.rem.backend.propertymanagement.entity.PropertyPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyPurchaseRepo extends JpaRepository<PropertyPurchase, Long> {
    Optional<PropertyPurchase> findByIdAndOrganizationId(long id, long organizationId);

    List<PropertyPurchase> findAllByOrganizationIdOrderByCreatedDateDesc(long organizationId);
}

