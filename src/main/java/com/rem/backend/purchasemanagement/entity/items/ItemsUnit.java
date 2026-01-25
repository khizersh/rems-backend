package com.rem.backend.purchasemanagement.entity.items;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class ItemsUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private long organizationId;

    @Column(nullable = false)
    private String name;   // Kilogram, Ton, Bag

    @Column(nullable = false)
    private String symbol; // kg, ton, bag


    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;
}
