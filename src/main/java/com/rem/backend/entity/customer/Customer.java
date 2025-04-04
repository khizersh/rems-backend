package com.rem.backend.entity.customer;

import com.rem.backend.entity.project.Project;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long customerId;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = true)
    private String country;
    @Column(nullable = true)
    private String city;
    @Column(nullable = true)
    private String address;
    @Column(nullable = false)
    private String nationalId;
    @Column(nullable = false)
    private String nextOFKinName;
    @Column(nullable = false)
    private String nextOFKinNationalId;
    @Column(nullable = false)
    private String relationShipWithKin;
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

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
