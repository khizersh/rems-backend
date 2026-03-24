package com.rem.backend.payrollmanagement.repository;

import com.rem.backend.payrollmanagement.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, String status);
    Page<LeaveRequest> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<LeaveRequest> findByOrganizationIdAndStatus(Long organizationId, String status, Pageable pageable);
    long countByEmployeeIdAndStatusAndLeaveType(Long employeeId, String status, String leaveType);
}
