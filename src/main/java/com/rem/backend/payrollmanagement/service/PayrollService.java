package com.rem.backend.payrollmanagement.service;

import com.rem.backend.orgaccountmanagement.entity.OrganizationAccount;
import com.rem.backend.orgaccountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.orgaccountmanagement.enums.TransactionCategory;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.payrollmanagement.dto.PayrollDashboardDTO;
import com.rem.backend.payrollmanagement.dto.ProcessPayrollRequest;
import com.rem.backend.payrollmanagement.entity.*;
import com.rem.backend.payrollmanagement.repository.*;
import com.rem.backend.repository.OrganizationAccoutRepo;
import com.rem.backend.repository.OrganizationAccountDetailRepo;
import com.rem.backend.service.JournalEntryService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final AllowanceRepository allowanceRepository;
    private final DeductionRepository deductionRepository;
    private final SalaryAmendmentRepository salaryAmendmentRepository;
    private final SalarySlipRepository salarySlipRepository;
    private final PayrollRepository payrollRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final JournalEntryService journalEntryService;
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;

    /**
     * Process payroll for an entire organization for a given month/year.
     * Generates salary slips for all active employees.
     */
    @Transactional
    public Map<String, Object> processPayroll(ProcessPayrollRequest request , String loggedInUser) {
        try {
            Long orgId = request.getOrganizationId();
            Integer month = request.getPayrollMonth();
            Integer year = request.getPayrollYear();

            // Check if payroll already processed
            boolean alreadyProcessed = payrollRepository.existsByOrganizationIdAndPayrollMonthAndPayrollYear(orgId, month, year);
            if (alreadyProcessed) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Payroll already processed for " + month + "/" + year + ". Cancel existing payroll first.");
            }

            // Get all active employees
            List<Employee> activeEmployees = employeeRepository.findByOrganizationIdAndStatus(orgId, "ACTIVE");

            if (activeEmployees.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "No active employees found");
            }

            BigDecimal totalBasic = BigDecimal.ZERO;
            BigDecimal totalAllowancesSum = BigDecimal.ZERO;
            BigDecimal totalDeductionsSum = BigDecimal.ZERO;
            BigDecimal totalAmendmentsSum = BigDecimal.ZERO;
            BigDecimal totalNetSum = BigDecimal.ZERO;
            List<SalarySlip> generatedSlips = new ArrayList<>();

            for (Employee employee : activeEmployees) {
                // Check if slip already exists for this employee
                Optional<SalarySlip> existingSlip = salarySlipRepository.findByEmployeeIdAndSalaryMonthAndSalaryYear(
                        employee.getId(), month, year);
                if (existingSlip.isPresent()) {
                    continue; // Skip if already generated
                }

                BigDecimal basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : BigDecimal.ZERO;

                // Calculate total allowances
                List<Allowance> allowances = allowanceRepository.findByEmployeeIdAndIsActiveTrue(employee.getId());
                BigDecimal totalAllowances = allowances.stream()
                        .map(Allowance::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate total deductions
                List<Deduction> deductions = deductionRepository.findByEmployeeIdAndIsActiveTrue(employee.getId());
                BigDecimal totalDeductions = deductions.stream()
                        .map(Deduction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate amendments for this month
                List<SalaryAmendment> amendments = salaryAmendmentRepository
                        .findByEmployeeIdAndSalaryMonthAndSalaryYearAndStatus(employee.getId(), month, year, "APPROVED");

                BigDecimal totalAmendmentAdditions = amendments.stream()
                        .filter(a -> "ADDITION".equals(a.getAmendmentType()))
                        .map(SalaryAmendment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalAmendmentDeductions = amendments.stream()
                        .filter(a -> "DEDUCTION".equals(a.getAmendmentType()))
                        .map(SalaryAmendment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal netAmendments = totalAmendmentAdditions.subtract(totalAmendmentDeductions);

                // Gross = Basic + Allowances
                BigDecimal grossSalary = basicSalary.add(totalAllowances);

                // Net = Gross - Deductions + Net Amendments
                BigDecimal netSalary = grossSalary.subtract(totalDeductions).add(netAmendments);

                // Get department name
                String departmentName = "";
                if (employee.getDepartment() != null) {
                    departmentName = employee.getDepartment().getName();
                }

                SalarySlip slip = SalarySlip.builder()
                        .organizationId(orgId)
                        .employeeId(employee.getId())
                        .employeeName(employee.getFullName())
                        .employeeCode(employee.getEmployeeCode())
                        .departmentName(departmentName)
                        .designation(employee.getDesignation())
                        .salaryMonth(month)
                        .salaryYear(year)
                        .basicSalary(basicSalary)
                        .totalAllowances(totalAllowances)
                        .totalDeductions(totalDeductions)
                        .totalAmendments(netAmendments)
                        .grossSalary(grossSalary)
                        .netSalary(netSalary)
                        .status("GENERATED")
                        .build();

                SalarySlip savedSlip = salarySlipRepository.save(slip);
                generatedSlips.add(savedSlip);

                journalEntryService.createJournalEntryForSalarySlip(savedSlip, loggedInUser);

                totalBasic = totalBasic.add(basicSalary);
                totalAllowancesSum = totalAllowancesSum.add(totalAllowances);
                totalDeductionsSum = totalDeductionsSum.add(totalDeductions);
                totalAmendmentsSum = totalAmendmentsSum.add(netAmendments);
                totalNetSum = totalNetSum.add(netSalary);
            }

            // Create payroll record
            Payroll payroll = Payroll.builder()
                    .organizationId(orgId)
                    .payrollMonth(month)
                    .payrollYear(year)
                    .status("COMPLETED")
                    .totalEmployees(generatedSlips.size())
                    .totalBasicSalary(totalBasic)
                    .totalAllowances(totalAllowancesSum)
                    .totalDeductions(totalDeductionsSum)
                    .totalAmendments(totalAmendmentsSum)
                    .totalNetSalary(totalNetSum)
                    .processedBy(request.getProcessedBy())
                    .processedDate(LocalDateTime.now())
                    .build();

            Payroll savedPayroll = payrollRepository.save(payroll);

            Map<String, Object> result = new HashMap<>();
            result.put("payroll", savedPayroll);
            result.put("salarySlips", generatedSlips);

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Generate salary slip for a single employee
     */
    public Map<String, Object> generateSingleSalarySlip(Long employeeId, Integer month, Integer year) {
        try {
            Optional<Employee> optionalEmployee = employeeRepository.findById(employeeId);
            if (optionalEmployee.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Employee not found");
            }

            Employee employee = optionalEmployee.get();

            // Check if slip already exists
            Optional<SalarySlip> existingSlip = salarySlipRepository.findByEmployeeIdAndSalaryMonthAndSalaryYear(
                    employeeId, month, year);
            if (existingSlip.isPresent()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, existingSlip.get());
            }

            BigDecimal basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : BigDecimal.ZERO;

            List<Allowance> allowances = allowanceRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
            BigDecimal totalAllowances = allowances.stream()
                    .map(Allowance::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Deduction> deductions = deductionRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
            BigDecimal totalDeductions = deductions.stream()
                    .map(Deduction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<SalaryAmendment> amendments = salaryAmendmentRepository
                    .findByEmployeeIdAndSalaryMonthAndSalaryYearAndStatus(employeeId, month, year, "APPROVED");

            BigDecimal totalAmendmentAdditions = amendments.stream()
                    .filter(a -> "ADDITION".equals(a.getAmendmentType()))
                    .map(SalaryAmendment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAmendmentDeductions = amendments.stream()
                    .filter(a -> "DEDUCTION".equals(a.getAmendmentType()))
                    .map(SalaryAmendment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netAmendments = totalAmendmentAdditions.subtract(totalAmendmentDeductions);
            BigDecimal grossSalary = basicSalary.add(totalAllowances);
            BigDecimal netSalary = grossSalary.subtract(totalDeductions).add(netAmendments);

            String departmentName = "";
            if (employee.getDepartment() != null) {
                departmentName = employee.getDepartment().getName();
            }

            SalarySlip slip = SalarySlip.builder()
                    .organizationId(employee.getOrganizationId())
                    .employeeId(employee.getId())
                    .employeeName(employee.getFullName())
                    .employeeCode(employee.getEmployeeCode())
                    .departmentName(departmentName)
                    .designation(employee.getDesignation())
                    .salaryMonth(month)
                    .salaryYear(year)
                    .basicSalary(basicSalary)
                    .totalAllowances(totalAllowances)
                    .totalDeductions(totalDeductions)
                    .totalAmendments(netAmendments)
                    .grossSalary(grossSalary)
                    .netSalary(netSalary)
                    .status("GENERATED")
                    .build();

            SalarySlip saved = salarySlipRepository.save(slip);

            // Build detailed response with breakdowns
            Map<String, Object> detail = new HashMap<>();
            detail.put("salarySlip", saved);
            detail.put("allowanceBreakdown", allowances);
            detail.put("deductionBreakdown", deductions);
            detail.put("amendments", amendments);

            return ResponseMapper.buildResponse(Responses.SUCCESS, detail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get salary slip by ID with full breakdown
     */
    public Map<String, Object> getSalarySlipById(Long id) {
        try {
            Optional<SalarySlip> optional = salarySlipRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Salary slip not found");
            }

            SalarySlip slip = optional.get();

            List<Allowance> allowances = allowanceRepository.findByEmployeeIdAndIsActiveTrue(slip.getEmployeeId());
            List<Deduction> deductions = deductionRepository.findByEmployeeIdAndIsActiveTrue(slip.getEmployeeId());
            List<SalaryAmendment> amendments = salaryAmendmentRepository
                    .findByEmployeeIdAndSalaryMonthAndSalaryYear(slip.getEmployeeId(), slip.getSalaryMonth(), slip.getSalaryYear());

            Map<String, Object> detail = new HashMap<>();
            detail.put("salarySlip", slip);
            detail.put("allowanceBreakdown", allowances);
            detail.put("deductionBreakdown", deductions);
            detail.put("amendments", amendments);

            return ResponseMapper.buildResponse(Responses.SUCCESS, detail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get all salary slips for an employee
     */
    public Map<String, Object> getSalarySlipsByEmployee(Long employeeId, Pageable pageable) {
        try {
            Page<SalarySlip> slips = salarySlipRepository.findByEmployeeId(employeeId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, slips);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get all salary slips for an organization for a given month/year
     */
    public Map<String, Object> getSalarySlipsByOrgAndMonth(Long organizationId, Integer month, Integer year) {
        try {
            List<SalarySlip> slips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);
            return ResponseMapper.buildResponse(Responses.SUCCESS, slips);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Mark salary slip as paid and deduct from organization account
     */
    @Transactional
    public Map<String, Object> markSalarySlipPaid(Long id, Long organizationAccountId, String paidBy) {
        try {
            Optional<SalarySlip> optional = salarySlipRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Salary slip not found");
            }

            SalarySlip slip = optional.get();

            if ("PAID".equals(slip.getStatus())) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Salary slip is already marked as paid");
            }

            double paymentAmount = slip.getNetSalary() != null ? slip.getNetSalary().doubleValue() : 0.0;

            OrganizationAccount orgAccount = organizationAccountRepo
                    .findByIdAndOrganizationId(organizationAccountId, slip.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid organization account for this organization"));

            if (orgAccount.getTotalAmount() < paymentAmount) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Insufficient organization account balance. Available: " + orgAccount.getTotalAmount() + ", Required: " + paymentAmount);
            }

            double newBalance = orgAccount.getTotalAmount() - paymentAmount;
            orgAccount.setTotalAmount(newBalance);
            orgAccount.setUpdatedBy(paidBy);
            organizationAccountRepo.save(orgAccount);

            OrganizationAccountDetail accountDetail = new OrganizationAccountDetail();
            accountDetail.setOrganizationAcctId(orgAccount.getId());
            accountDetail.setTransactionType(TransactionType.CREDIT);
            accountDetail.setTransactionCategory(TransactionCategory.OTHER);
            accountDetail.setAmount(paymentAmount);
            accountDetail.setComments("Salary Payment: employee=" + slip.getEmployeeName()
                    + " slipId=" + slip.getId()
                    + " (" + slip.getSalaryMonth() + "/" + slip.getSalaryYear() + ")");
            accountDetail.setCreatedBy(paidBy);
            accountDetail.setUpdatedBy(paidBy);
            organizationAccountDetailRepo.save(accountDetail);

            slip.setStatus("PAID");
            slip.setPaidDate(LocalDateTime.now());
            salarySlipRepository.save(slip);

            journalEntryService.paySalaryJournalEntry(slip, organizationAccountId, paidBy);

            Map<String, Object> result = new HashMap<>();
            result.put("salarySlip", slip);
            result.put("orgAccountBalance", newBalance);
            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Mark all slips for a month as paid (bulk) and deduct total from organization account
     */
    @Transactional
    public Map<String, Object> markAllSlipsPaid(Long organizationId, Integer month, Integer year,
                                                Long organizationAccountId, String paidBy) {
        try {
            List<SalarySlip> slips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);

            List<SalarySlip> pendingSlips = slips.stream()
                    .filter(s -> "GENERATED".equals(s.getStatus()))
                    .toList();

            if (pendingSlips.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "No generated salary slips found to pay");
            }

            double totalPayable = pendingSlips.stream()
                    .mapToDouble(s -> s.getNetSalary() != null ? s.getNetSalary().doubleValue() : 0.0)
                    .sum();

            OrganizationAccount orgAccount = organizationAccountRepo
                    .findByIdAndOrganizationId(organizationAccountId, organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid organization account for this organization"));

            if (orgAccount.getTotalAmount() < totalPayable) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Insufficient organization account balance. Available: " + orgAccount.getTotalAmount()
                                + ", Required: " + totalPayable);
            }

            double newBalance = orgAccount.getTotalAmount() - totalPayable;
            orgAccount.setTotalAmount(newBalance);
            orgAccount.setUpdatedBy(paidBy);
            organizationAccountRepo.save(orgAccount);

            OrganizationAccountDetail accountDetail = new OrganizationAccountDetail();
            accountDetail.setOrganizationAcctId(orgAccount.getId());
            accountDetail.setTransactionType(TransactionType.CREDIT);
            accountDetail.setTransactionCategory(TransactionCategory.OTHER);
            accountDetail.setAmount(totalPayable);
            accountDetail.setComments("Bulk Salary Payment: " + pendingSlips.size()
                    + " employees (" + month + "/" + year + ")");
            accountDetail.setCreatedBy(paidBy);
            accountDetail.setUpdatedBy(paidBy);
            organizationAccountDetailRepo.save(accountDetail);

            int paidCount = 0;
            for (SalarySlip slip : pendingSlips) {
                slip.setStatus("PAID");
                slip.setPaidDate(LocalDateTime.now());
                salarySlipRepository.save(slip);
                journalEntryService.paySalaryJournalEntry(slip, organizationAccountId, paidBy);
                paidCount++;
            }

            Optional<Payroll> payrollOpt = payrollRepository.findByOrganizationIdAndPayrollMonthAndPayrollYear(organizationId, month, year);
            payrollOpt.ifPresent(payroll -> {
                payroll.setStatus("COMPLETED");
                payrollRepository.save(payroll);
            });

            Map<String, Object> result = new HashMap<>();
            result.put("paidCount", paidCount);
            result.put("totalSlips", slips.size());
            result.put("totalAmountPaid", totalPayable);
            result.put("orgAccountBalance", newBalance);
            return ResponseMapper.buildResponse(Responses.SUCCESS, result);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payroll history for an organization
     */
    public Map<String, Object> getPayrollHistory(Long organizationId, Pageable pageable) {
        try {
            Page<Payroll> payrolls = payrollRepository.findByOrganizationId(organizationId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, payrolls);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Cancel a payroll run and remove all associated salary slips
     */
    @Transactional
    public Map<String, Object> cancelPayroll(Long payrollId) {
        try {
            Optional<Payroll> optional = payrollRepository.findById(payrollId);
            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Payroll not found");
            }

            Payroll payroll = optional.get();

            // Delete all salary slips for this payroll month
            List<SalarySlip> slips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(
                    payroll.getOrganizationId(), payroll.getPayrollMonth(), payroll.getPayrollYear());

            // Check if any slips are already paid
            boolean anyPaid = slips.stream().anyMatch(s -> "PAID".equals(s.getStatus()));
            if (anyPaid) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER,
                        "Cannot cancel payroll. Some salary slips are already marked as paid.");
            }

            salarySlipRepository.deleteAll(slips);

            payroll.setStatus("CANCELLED");
            payrollRepository.save(payroll);

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Payroll cancelled successfully. " + slips.size() + " salary slips removed.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * HR & Payroll Dashboard - summary statistics
     */
    public Map<String, Object> getDashboard(Long organizationId) {
        try {
            long totalEmployees = employeeRepository.countByOrganizationId(organizationId);
            long activeEmployees = employeeRepository.countByOrganizationIdAndStatus(organizationId, "ACTIVE");
            long totalDepartments = departmentRepository.countByOrganizationId(organizationId);

            // Current month payroll total
            LocalDate now = LocalDate.now();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();

            List<SalarySlip> currentSlips = salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(
                    organizationId, currentMonth, currentYear);
            BigDecimal totalMonthlyPayroll = currentSlips.stream()
                    .map(SalarySlip::getNetSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long payslipsThisMonth = currentSlips.size();

            // Today's attendance
            LocalDate today = LocalDate.now();
            List<Attendance> todayAttendance = attendanceRepository.findByOrganizationIdAndAttendanceDate(organizationId, today);
            long presentToday = todayAttendance.stream()
                    .filter(a -> "PRESENT".equals(a.getStatus()) || "LATE".equals(a.getStatus()))
                    .count();
            long absentToday = todayAttendance.stream()
                    .filter(a -> "ABSENT".equals(a.getStatus()))
                    .count();

            // Pending leave requests - count all pending
            Page<com.rem.backend.payrollmanagement.entity.LeaveRequest> pendingLeaves =
                    leaveRequestRepository.findByOrganizationIdAndStatus(organizationId, "PENDING",
                            org.springframework.data.domain.PageRequest.of(0, 1));
            long pendingLeaveRequests = pendingLeaves.getTotalElements();

            PayrollDashboardDTO dashboard = PayrollDashboardDTO.builder()
                    .totalEmployees(totalEmployees)
                    .activeEmployees(activeEmployees)
                    .totalDepartments(totalDepartments)
                    .totalMonthlyPayroll(totalMonthlyPayroll)
                    .pendingLeaveRequests(pendingLeaveRequests)
                    .presentToday(presentToday)
                    .absentToday(absentToday)
                    .payslipsGeneratedThisMonth(payslipsThisMonth)
                    .build();

            return ResponseMapper.buildResponse(Responses.SUCCESS, dashboard);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get department-wise salary summary
     */
    public Map<String, Object> getDepartmentSalarySummary(Long organizationId) {
        try {
            List<Department> departments = departmentRepository.findByOrganizationId(organizationId);
            List<Map<String, Object>> summaryList = new ArrayList<>();

            for (Department dept : departments) {
                List<Employee> employees = employeeRepository.findByDepartmentIdAndStatus(dept.getId(), "ACTIVE");
                BigDecimal totalBasic = employees.stream()
                        .map(e -> e.getBasicSalary() != null ? e.getBasicSalary() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalAllowances = BigDecimal.ZERO;
                BigDecimal totalDeductions = BigDecimal.ZERO;

                for (Employee emp : employees) {
                    List<Allowance> allowances = allowanceRepository.findByEmployeeIdAndIsActiveTrue(emp.getId());
                    totalAllowances = totalAllowances.add(allowances.stream()
                            .map(Allowance::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

                    List<Deduction> deductions = deductionRepository.findByEmployeeIdAndIsActiveTrue(emp.getId());
                    totalDeductions = totalDeductions.add(deductions.stream()
                            .map(Deduction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }

                BigDecimal totalNet = totalBasic.add(totalAllowances).subtract(totalDeductions);

                Map<String, Object> deptSummary = new HashMap<>();
                deptSummary.put("departmentId", dept.getId());
                deptSummary.put("departmentName", dept.getName());
                deptSummary.put("employeeCount", employees.size());
                deptSummary.put("totalBasicSalary", totalBasic);
                deptSummary.put("totalAllowances", totalAllowances);
                deptSummary.put("totalDeductions", totalDeductions);
                deptSummary.put("totalNetSalary", totalNet);
                deptSummary.put("budgetAllocated", dept.getBudgetAllocated());

                summaryList.add(deptSummary);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, summaryList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
