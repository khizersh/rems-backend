package com.rem.backend.usermanagement.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "role_mapping")
@Data
public class UserRoleMapper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Code cannot be empty")
    private String roleCode;

    @NotBlank(message = "Code cannot be empty")
    private String endPoint;

}
