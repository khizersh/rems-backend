package com.rem.backend.usermanagement.entity;


import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "user_role")
@Data
public class UserRoles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private long userId;

    @Column(nullable = true)
    private String roleCode;

}
