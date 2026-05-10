package com.rem.backend.propertymanagement.repository;

import com.rem.backend.propertymanagement.entity.PropertyAsset;
import com.rem.backend.propertymanagement.enums.PropertyAssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyAssetRepo extends JpaRepository<PropertyAsset, Long> {
    Optional<PropertyAsset> findByPropertyPurchaseId(long propertyPurchaseId);

    boolean existsByIdAndStatus(long id, PropertyAssetStatus status);
}

