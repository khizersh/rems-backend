package com.rem.backend.propertymanagement.repository;

import com.rem.backend.propertymanagement.entity.PropertySeller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertySellerRepo extends JpaRepository<PropertySeller, Long> {
    Optional<PropertySeller> findByIdAndIsActiveTrue(long id);

    List<PropertySeller> findAllByOrganizationIdAndIsActiveTrueOrderByCreatedDateDesc(long organizationId);
}

