package com.rem.backend.payrollmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "employee_attendance")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    private String status; // PRESENT, ABSENT, HALF_DAY, LATE, ON_LEAVE

    private Double hoursWorked;

    private String remarks;

    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) this.status = "PRESENT";
    }
}
