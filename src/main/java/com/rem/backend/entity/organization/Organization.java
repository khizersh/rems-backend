package com.rem.backend.entity.organization;

import com.rem.backend.entity.project.Project;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long organizationId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String logo;
    @Column(nullable = false)
    private String updatedBy;
    @Column(nullable = false)
    private String createdBy;
    @Column(nullable = false)
    private Timestamp createdDate;
    @Column(nullable = false)
    private Timestamp updatedDate;


    @PrePersist
    public void prePersist() {
        this.createdDate = new Timestamp(System.currentTimeMillis());
        this.updatedDate = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = new Timestamp(System.currentTimeMillis());
    }




}
