package com.rem.backend.usermanagement.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isActive = true;

    @Transient
    private Set<UserRoles> roles;

}