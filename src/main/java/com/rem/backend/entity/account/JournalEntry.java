package com.rem.backend.entity.account;

import com.rem.backend.enums.JournalEntryStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entry")
@Data
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long organizationId;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = true)
    private String referenceType;

    @Column(nullable = true)
    private Long projectId;

    @Column(nullable = true)
    private Long unitId;

    @Column(nullable = true)
    private Long customerId;

    @Column(nullable = true)
    private Long vendorId;

    @Column(nullable = true)
    private Long organizationAccountId;

    @Column(nullable = true)
    private Long expenseId;

    @Column(nullable = true)
    private Long bookingId;

    @Column(nullable = true)
    private Long additionalReferenceId;

    @Column(nullable = true)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
    }
}


