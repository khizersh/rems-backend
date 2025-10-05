package com.rem.backend.entity.organization;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization")
@Data
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long organizationId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String contactNo;
    @Column(nullable = true)
    private String logo;
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isActive = true;
    @Column(nullable = false)
    private String updatedBy;
    @Column(nullable = false)
    private String createdBy;
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
