package com.rem.backend.entity.customer;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Data
public class Customer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long customerId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

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
    private String guardianName;

    @Column(nullable = false)
    private String contactNo;

    @Column(nullable = false)
    private String nextOFKinNationalId;

    @Column(nullable = false)
    private String relationShipWithKin;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private long unitCount;


    @Column(nullable = false)
    private int age;


    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Transient
    private String email;

    @Transient
    private String username;

    @Transient
    private String password;

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
        if (this.updatedDate == null) {
            this.updatedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.updatedDate == null) {
            this.updatedDate = LocalDateTime.now();
        }
    }

}
