package com.rem.backend.payrollmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String leaveType; // ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer totalDays;

    private String reason;

    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    private String approvedBy;

    private LocalDateTime approvedDate;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.totalDays == null && this.startDate != null && this.endDate != null) {
            this.totalDays = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
