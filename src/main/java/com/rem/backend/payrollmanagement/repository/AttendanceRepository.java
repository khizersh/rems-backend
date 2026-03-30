package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Page<Attendance> findByEmployeeId(Long employeeId, Pageable pageable);
    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByOrganizationIdAndAttendanceDate(Long organizationId, LocalDate date);
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);
    long countByEmployeeIdAndStatusAndAttendanceDateBetween(Long employeeId, String status, LocalDate startDate, LocalDate endDate);
    Page<Attendance> findByOrganizationIdAndAttendanceDateBetween(Long organizationId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
