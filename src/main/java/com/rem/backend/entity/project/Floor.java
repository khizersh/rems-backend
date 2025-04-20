package com.rem.backend.entity.project;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "floor")
@Data
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private int floor;

    @Column(nullable = false)
    private long projectId;

    @Transient
    private String projectName;

    @Transient
    private int unitCount;

    @Transient
    private List<Unit> unitList = new ArrayList<>();

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