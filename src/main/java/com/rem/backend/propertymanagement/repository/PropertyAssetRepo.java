package com.rem.backend.propertymanagement.repository;

import com.rem.backend.propertymanagement.entity.PropertyAsset;
import com.rem.backend.propertymanagement.enums.PropertyAssetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyAssetRepo extends JpaRepository<PropertyAsset, Long> {
    Optional<PropertyAsset> findByPropertyPurchaseId(long propertyPurchaseId);

    boolean existsByIdAndStatus(long id, PropertyAssetStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PropertyAsset a WHERE a.propertyPurchaseId = :purchaseId")
    Optional<PropertyAsset> findByPropertyPurchaseIdForUpdate(@Param("purchaseId") long purchaseId);
}
