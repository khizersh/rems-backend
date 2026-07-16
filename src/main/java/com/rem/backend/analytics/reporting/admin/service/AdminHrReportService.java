package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.admin.query.AdminWorkforceReportRepository;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.payrollmanagement.entity.SalarySlip;
import com.rem.backend.payrollmanagement.repository.DepartmentRepository;
import com.rem.backend.payrollmanagement.repository.EmployeeRepository;
import com.rem.backend.payrollmanagement.repository.SalarySlipRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin reporting for HR / workforce and system users.
 * Payroll cost is shown from the payroll sub-system and cross-checked against the salary
 * expense / payable GL accounts.
 */
@Service
@AllArgsConstructor
public class AdminHrReportService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SalarySlipRepository salarySlipRepository;
    private final AdminWorkforceReportRepository workforceReportRepo;
    private final AdminFinancialReportService financialReportService;

    public ReportingView hrOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        long totalEmployees = employeeRepository.countByOrganizationId(organizationId);
        long activeEmployees = employeeRepository.countByOrganizationIdAndStatus(organizationId, STATUS_ACTIVE);
        long departments = departmentRepository.countByOrganizationId(organizationId);

        // Current-month payroll cost (or the filter's month/year if supplied).
        int year = filter != null && filter.getYear() != null ? filter.getYear() : LocalDate.now().getYear();
        int month = filter != null && filter.getMonth() != null ? filter.getMonth()
                : range.getEnd().getMonthValue();
        double monthPayroll = sumNetSalary(
                salarySlipRepository.findByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year));
        long slipsGenerated = salarySlipRepository.countByOrganizationIdAndSalaryMonthAndSalaryYear(organizationId, month, year);

        double salaryExpense = financialReportService.codeBalance(organizationId, range, JournalUtilities.SALARY_EXPENSE);
        double salaryPayable = financialReportService.codeBalance(organizationId, range, JournalUtilities.SALARY_PAYABLE);

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("totalEmployees", "Total Employees", totalEmployees, "FaUsers", ColorHint.PRIMARY),
                ReportingFactory.countCard("activeEmployees", "Active Employees", activeEmployees, "FaUserCheck", ColorHint.SUCCESS),
                ReportingFactory.countCard("departments", "Departments", departments, "FaSitemap", ColorHint.INFO),
                ReportingFactory.currencyCard("monthPayroll", "Payroll (Selected Month)", monthPayroll, "FaMoneyCheckDollar", ColorHint.WARNING),
                ReportingFactory.countCard("slipsGenerated", "Salary Slips (Month)", slipsGenerated, "FaFileInvoice", ColorHint.NEUTRAL),
                ReportingFactory.currencyCard("salaryExpense", "Salary Expense (Period)", salaryExpense, "FaReceipt", ColorHint.DANGER),
                ReportingFactory.currencyCard("salaryPayable", "Salary Payable", salaryPayable, "FaClock", ColorHint.WARNING)
        );

        GroupedSummary workforce = GroupedSummary.builder()
                .key("workforce").label("Workforce")
                .total(totalEmployees)
                .items(List.of(
                        ReportingFactory.countValue("active", "Active", activeEmployees, ColorHint.SUCCESS),
                        ReportingFactory.countValue("inactive", "Other", Math.max(0, totalEmployees - activeEmployees), ColorHint.NEUTRAL)))
                .build();

        return ReportingView.builder()
                .key("hr-overview").title("HR Overview")
                .subtitle("Workforce & payroll")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(workforce))
                .charts(List.of())
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView userOverview(long organizationId, ReportingFilter filter) {
        long activeUsers = workforceReportRepo.countActiveUsers(organizationId);

        List<KpiCard> cards = List.of(
                ReportingFactory.countCard("activeUsers", "Active System Users", activeUsers, "FaUserGear", ColorHint.PRIMARY)
        );

        return ReportingView.builder()
                .key("user-overview").title("User Overview")
                .subtitle("System user accounts")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    private static double sumNetSalary(List<SalarySlip> slips) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SalarySlip slip : slips) {
            if (slip.getNetSalary() != null) {
                sum = sum.add(slip.getNetSalary());
            }
        }
        return sum.doubleValue();
    }
}
