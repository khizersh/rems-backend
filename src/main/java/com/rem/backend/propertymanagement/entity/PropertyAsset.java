package com.rem.backend.propertymanagement.entity;

import com.rem.backend.propertymanagement.enums.PropertyAssetStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_asset")
@Data
public class PropertyAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long organizationId;

    @Column(nullable = false)
    private long propertyPurchaseId;

    @Column(nullable = false)
    private double acquiredAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyAssetStatus status = PropertyAssetStatus.AVAILABLE;

    @Column(nullable = true)
    private Long usedInProjectId;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}

