package com.rem.backend.entity.project;

import com.rem.backend.entity.organization.Organization;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long projectId;
    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
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
