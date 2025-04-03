package com.rem.backend.entity.project;

import com.rem.backend.entity.organization.Organization;
import jakarta.persistence.*;

import java.sql.Timestamp;

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
    private String updatedBy;
    @Column(nullable = false)
    private String createdBy;
    @Column(nullable = false)
    private Timestamp createdDate;
    @Column(nullable = false)
    private Timestamp updatedDate;


}
