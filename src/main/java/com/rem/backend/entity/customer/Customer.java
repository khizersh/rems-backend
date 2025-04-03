package com.rem.backend.entity.customer;

import com.rem.backend.entity.project.Project;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

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
    @Column(nullable = false)
    private String updatedBy;
    @Column(nullable = false)
    private String createdBy;
    @Column(nullable = false)
    private Timestamp createdDate;
    @Column(nullable = false)
    private Timestamp updatedDate;
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
