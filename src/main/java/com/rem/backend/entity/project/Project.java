package com.rem.backend.entity.project;

import com.rem.backend.enums.ProjectType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Data
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long projectId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private long floors;

    @Column(nullable = false)
    private double purchasingAmount = 0.0;

    @Column(nullable = false)
    private double registrationAmount = 0.0;


    @Column(nullable = false)
    private double constructionAmount = 0.0;

    @Column(nullable = false)
    private double additionalAmount = 0.0;

    @Column(nullable = false)
    private double totalAmount = 0.0;

    @Column(nullable = true)
    private String information;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectType projectType;

    @Column(nullable = false)
    private long organizationId;

    @Column(nullable = false)
    private int monthDuration;


    @Transient
    private List<Floor> floorList = new ArrayList<>();

    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isActive = true;

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
