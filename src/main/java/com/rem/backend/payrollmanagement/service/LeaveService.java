package com.rem.backend.payrollmanagement.service;

import com.rem.backend.payrollmanagement.dto.LeaveRequestDTO;
import com.rem.backend.payrollmanagement.entity.LeaveRequest;
import com.rem.backend.payrollmanagement.repository.LeaveRequestRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;

    public Map<String, Object> applyLeave(LeaveRequestDTO request) {
        try {
            int totalDays = (int) (request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay()) + 1;

            if (totalDays <= 0) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "End date must be after start date");
            }

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .organizationId(request.getOrganizationId())
                    .employeeId(request.getEmployeeId())
                    .leaveType(request.getLeaveType())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .totalDays(totalDays)
                    .reason(request.getReason())
                    .status("PENDING")
                    .build();

            LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> approveLeave(Long id, String approvedBy) {
        try {
            Optional<LeaveRequest> optional = leaveRequestRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Leave request not found");
            }

            LeaveRequest leaveRequest = optional.get();
            if (!"PENDING".equals(leaveRequest.getStatus())) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Leave request is already " + leaveRequest.getStatus());
            }

            leaveRequest.setStatus("APPROVED");
            leaveRequest.setApprovedBy(approvedBy);
            leaveRequest.setApprovedDate(LocalDateTime.now());

            LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> rejectLeave(Long id, String rejectedBy) {
        try {
            Optional<LeaveRequest> optional = leaveRequestRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Leave request not found");
            }

            LeaveRequest leaveRequest = optional.get();
            if (!"PENDING".equals(leaveRequest.getStatus())) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Leave request is already " + leaveRequest.getStatus());
            }

            leaveRequest.setStatus("REJECTED");
            leaveRequest.setApprovedBy(rejectedBy);
            leaveRequest.setApprovedDate(LocalDateTime.now());

            LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> cancelLeave(Long id) {
        try {
            Optional<LeaveRequest> optional = leaveRequestRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Leave request not found");
            }

            LeaveRequest leaveRequest = optional.get();
            leaveRequest.setStatus("CANCELLED");

            LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLeavesByEmployee(Long employeeId, Pageable pageable) {
        try {
            Page<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(employeeId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, leaves);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLeavesByOrganization(Long organizationId, Pageable pageable) {
        try {
            Page<LeaveRequest> leaves = leaveRequestRepository.findByOrganizationId(organizationId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, leaves);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getPendingLeaves(Long organizationId, Pageable pageable) {
        try {
            Page<LeaveRequest> leaves = leaveRequestRepository.findByOrganizationIdAndStatus(organizationId, "PENDING", pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, leaves);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getLeaveById(Long id) {
        try {
            Optional<LeaveRequest> optional = leaveRequestRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Leave request not found");
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, optional.get());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> deleteLeave(Long id) {
        try {
            if (!leaveRequestRepository.existsById(id)) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Leave request not found");
            }
            leaveRequestRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Leave request deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
