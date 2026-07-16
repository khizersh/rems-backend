package com.rem.backend.analytics.download.admin.service;

import com.rem.backend.analytics.download.admin.query.AdminAccountingDownloadRepository;
import com.rem.backend.analytics.download.admin.query.projection.LedgerLineProjection;
import com.rem.backend.analytics.download.shared.dto.FinancialStatementReport;
import com.rem.backend.analytics.download.shared.dto.ReportCatalogItem;
import com.rem.backend.analytics.download.shared.dto.ReportLine;
import com.rem.backend.analytics.download.shared.dto.ReportSection;
import com.rem.backend.analytics.download.shared.dto.ReportTotals;
import com.rem.backend.analytics.download.shared.enums.AccountingReportType;
import com.rem.backend.analytics.download.shared.filters.DownloadFilter;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountSummaryProjection;
import com.rem.backend.analytics.reporting.shared.dto.ExceptionItem;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.Severity;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;
import com.rem.backend.organizationmanagement.entity.Organization;
import com.rem.backend.organizationmanagement.repository.OrganizationRepo;
import com.rem.backend.utility.Utility;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Builds downloadable accounting statements from posted journal data.
 */
@Service
@AllArgsConstructor
public class AdminAccountingDownloadService {

    private final AdminAccountingDownloadRepository downloadRepo;
    private final OrganizationRepo organizationRepo;

    public List<ReportCatalogItem> catalog() {
        List<ReportCatalogItem> items = new ArrayList<>();
        for (AccountingReportType type : AccountingReportType.values()) {
            items.add(ReportCatalogItem.builder()
                    .key(type.getKey())
                    .title(type.getTitle())
                    .description(type.getDescription())
                    .supportedFormats(List.of("JSON", "CSV"))
                    .endpoint("/reporting/admin/downloads/" + type.getKey())
                    .build());
        }
        return items;
    }

    public FinancialStatementReport trialBalance(long organizationId, DownloadFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<ReportLine> lines = AccountingDownloadSupport.buildTrialBalanceLines(
                downloadRepo.summarizeByAccountBefore(organizationId, range.getStart()),
                downloadRepo.summarizeByAccountPeriod(organizationId, range.getStart(), range.getEnd()),
                downloadRepo.summarizeByAccountAsOf(organizationId, range.getEnd()));

        double totalOpening = 0, totalDebit = 0, totalCredit = 0, totalClosing = 0;
        for (ReportLine line : lines) {
            totalOpening += nz(line.getOpeningBalance());
            totalDebit += nz(line.getDebit());
            totalCredit += nz(line.getCredit());
            totalClosing += nz(line.getClosingBalance());
        }

        ReportTotals totals = ReportTotals.builder()
                .totalOpening(ReportingFormatUtil.round2(totalOpening))
                .totalDebit(ReportingFormatUtil.round2(totalDebit))
                .totalCredit(ReportingFormatUtil.round2(totalCredit))
                .totalClosing(ReportingFormatUtil.round2(totalClosing))
                .difference(ReportingFormatUtil.round2(totalDebit - totalCredit))
                .build();

        return baseReport(organizationId, AccountingReportType.TRIAL_BALANCE, filter,
                "Posted ledger balances with opening, movement and closing",
                rangeSubtitle(range),
                List.of(section("accounts", "All Accounts", lines, null)),
                totals,
                trialBalanceAlerts(totalDebit, totalCredit));
    }

    public FinancialStatementReport profitLoss(long organizationId, DownloadFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<AccountSummaryProjection> period =
                downloadRepo.summarizeByAccountPeriod(organizationId, range.getStart(), range.getEnd());

        List<ReportLine> incomeLines = AccountingDownloadSupport.buildStatementLines(
                period, AccountingDownloadSupport::isIncomeType);
        List<ReportLine> expenseLines = AccountingDownloadSupport.buildStatementLines(
                period, AccountingDownloadSupport::isExpenseType);

        double totalIncome = AccountingDownloadSupport.sumAmounts(incomeLines);
        double totalExpense = AccountingDownloadSupport.sumAmounts(expenseLines);
        double netProfit = ReportingFormatUtil.round2(totalIncome - totalExpense);

        ReportTotals totals = ReportTotals.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .build();

        return baseReport(organizationId, AccountingReportType.PROFIT_LOSS, filter,
                "Income and expense for the selected period",
                rangeSubtitle(range),
                List.of(
                        section("income", "Income", incomeLines, totalIncome),
                        section("expense", "Expenses", expenseLines, totalExpense)),
                totals,
                List.of());
    }

    public FinancialStatementReport balanceSheet(long organizationId, DownloadFilter filter) {
        LocalDateTime asOf = resolveAsOfDate(filter);
        List<AccountSummaryProjection> accounts =
                downloadRepo.summarizeByAccountAsOf(organizationId, asOf);

        List<ReportLine> assetLines = AccountingDownloadSupport.buildStatementLines(
                accounts, AccountingDownloadSupport::isAssetType);
        List<ReportLine> liabilityLines = AccountingDownloadSupport.buildStatementLines(
                accounts, AccountingDownloadSupport::isLiabilityType);
        List<ReportLine> equityLines = AccountingDownloadSupport.buildStatementLines(
                accounts, AccountingDownloadSupport::isEquityType);

        double totalAssets = AccountingDownloadSupport.sumAmounts(assetLines);
        double totalLiabilities = AccountingDownloadSupport.sumAmounts(liabilityLines);
        double totalEquity = AccountingDownloadSupport.sumAmounts(equityLines);

        // Unclosed P&L: cumulative income minus expense as of date
        double incomeTotal = AccountingDownloadSupport.sumAmounts(
                AccountingDownloadSupport.buildStatementLines(accounts, AccountingDownloadSupport::isIncomeType));
        double expenseTotal = AccountingDownloadSupport.sumAmounts(
                AccountingDownloadSupport.buildStatementLines(accounts, AccountingDownloadSupport::isExpenseType));
        double unclosedProfit = ReportingFormatUtil.round2(incomeTotal - expenseTotal);

        if (Math.abs(unclosedProfit) >= 0.005) {
            equityLines = new ArrayList<>(equityLines);
            equityLines.add(ReportLine.builder()
                    .accountCode("COMPUTED")
                    .accountName("Current Period Net Profit (Unclosed)")
                    .accountType("EQUITY")
                    .accountCategory("RETAINED EARNINGS")
                    .amount(unclosedProfit)
                    .build());
            totalEquity = ReportingFormatUtil.round2(totalEquity + unclosedProfit);
        }

        ReportTotals totals = ReportTotals.builder()
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .netProfit(unclosedProfit)
                .difference(ReportingFormatUtil.round2(totalAssets - (totalLiabilities + totalEquity)))
                .build();

        String subtitle = "As of " + (filter != null && filter.getAsOfDate() != null && !filter.getAsOfDate().isBlank()
                ? filter.getAsOfDate() : formatAsOf(asOf));

        List<ExceptionItem> alerts = new ArrayList<>();
        if (Math.abs(nz(totals.getDifference())) > 0.01) {
            alerts.add(ExceptionItem.builder()
                    .key("balanceSheetImbalance")
                    .severity(Severity.WARNING)
                    .title("Balance sheet out of balance")
                    .description("Assets do not equal liabilities plus equity.")
                    .value(totals.getDifference())
                    .formattedValue(ReportingFormatUtil.compact(totals.getDifference()))
                    .colorHint(ColorHint.WARNING)
                    .build());
        }

        return baseReport(organizationId, AccountingReportType.BALANCE_SHEET, filter,
                "Assets, liabilities and equity as of a date",
                subtitle,
                List.of(
                        section("assets", "Assets", assetLines, totalAssets),
                        section("liabilities", "Liabilities", liabilityLines, totalLiabilities),
                        section("equity", "Equity", equityLines, totalEquity)),
                totals,
                alerts);
    }

    public FinancialStatementReport generalLedger(long organizationId, DownloadFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        String accountCode = filter != null ? filter.getAccountCode() : null;
        List<LedgerLineProjection> raw = downloadRepo.ledgerLines(
                organizationId, range.getStart(), range.getEnd(), accountCode);

        return buildLedgerReport(organizationId, AccountingReportType.GENERAL_LEDGER, filter, range, raw,
                accountCode == null ? "All accounts" : "Account " + accountCode);
    }

    public FinancialStatementReport accountStatement(long organizationId, DownloadFilter filter) {
        if (filter == null || filter.getAccountCode() == null || filter.getAccountCode().isBlank()) {
            throw new IllegalArgumentException("accountCode is required for account statement");
        }
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<LedgerLineProjection> raw = downloadRepo.ledgerLines(
                organizationId, range.getStart(), range.getEnd(), filter.getAccountCode());

        return buildLedgerReport(organizationId, AccountingReportType.ACCOUNT_STATEMENT, filter, range, raw,
                filter.getAccountCode());
    }

    public FinancialStatementReport journalRegister(long organizationId, DownloadFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<LedgerLineProjection> raw = downloadRepo.journalRegisterLines(
                organizationId, range.getStart(), range.getEnd());

        List<ReportLine> lines = new ArrayList<>();
        for (LedgerLineProjection row : raw) {
            lines.add(ReportLine.builder()
                    .journalEntryId(row.getJournalEntryId())
                    .transactionDate(row.getTransactionDate())
                    .referenceType(row.getReferenceType())
                    .journalDescription(row.getJournalDescription())
                    .description(row.getLineDescription())
                    .accountCode(row.getAccountCode())
                    .accountName(row.getAccountName())
                    .accountType(row.getAccountType())
                    .accountCategory(row.getAccountCategory())
                    .debit(ReportingFormatUtil.round2(nz(row.getDebitAmount())))
                    .credit(ReportingFormatUtil.round2(nz(row.getCreditAmount())))
                    .build());
        }

        double totalDebit = lines.stream().mapToDouble(l -> nz(l.getDebit())).sum();
        double totalCredit = lines.stream().mapToDouble(l -> nz(l.getCredit())).sum();

        ReportTotals totals = ReportTotals.builder()
                .totalDebit(ReportingFormatUtil.round2(totalDebit))
                .totalCredit(ReportingFormatUtil.round2(totalCredit))
                .difference(ReportingFormatUtil.round2(totalDebit - totalCredit))
                .build();

        return baseReport(organizationId, AccountingReportType.JOURNAL_REGISTER, filter,
                "All posted journal vouchers in the period",
                rangeSubtitle(range),
                List.of(section("journalRegister", "Journal Register", lines, null)),
                totals,
                trialBalanceAlerts(totalDebit, totalCredit));
    }

    // -------------------------------------------------------------------------

    private FinancialStatementReport buildLedgerReport(
            long organizationId,
            AccountingReportType type,
            DownloadFilter filter,
            DateRange range,
            List<LedgerLineProjection> raw,
            String scopeLabel) {

        Map<String, List<ReportLine>> byAccount = new LinkedHashMap<>();
        Map<String, String> accountLabels = new LinkedHashMap<>();
        Map<String, String> accountTypes = new LinkedHashMap<>();

        for (LedgerLineProjection row : raw) {
            String code = row.getAccountCode() != null ? row.getAccountCode() : "UNKNOWN";
            accountLabels.putIfAbsent(code, row.getAccountName());
            accountTypes.putIfAbsent(code, row.getAccountType());
            byAccount.computeIfAbsent(code, k -> new ArrayList<>()).add(ReportLine.builder()
                    .journalEntryId(row.getJournalEntryId())
                    .transactionDate(row.getTransactionDate())
                    .referenceType(row.getReferenceType())
                    .journalDescription(row.getJournalDescription())
                    .description(row.getLineDescription())
                    .accountCode(row.getAccountCode())
                    .accountName(row.getAccountName())
                    .accountType(row.getAccountType())
                    .accountCategory(row.getAccountCategory())
                    .debit(ReportingFormatUtil.round2(nz(row.getDebitAmount())))
                    .credit(ReportingFormatUtil.round2(nz(row.getCreditAmount())))
                    .build());
        }

        List<ReportSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<ReportLine>> entry : byAccount.entrySet()) {
            String code = entry.getKey();
            String typeName = accountTypes.getOrDefault(code, "");
            double running = 0d;
            for (ReportLine line : entry.getValue()) {
                running += AccountingDownloadSupport.naturalBalance(
                        typeName, nz(line.getDebit()), nz(line.getCredit()));
                line.setRunningBalance(ReportingFormatUtil.round2(running));
            }
            String label = code + " — " + accountLabels.getOrDefault(code, "");
            sections.add(section(code, label, entry.getValue(), null));
        }

        return baseReport(organizationId, type, filter,
                type.getDescription(),
                rangeSubtitle(range) + " | " + scopeLabel,
                sections,
                ReportTotals.builder().build(),
                List.of());
    }

    private FinancialStatementReport baseReport(
            long organizationId,
            AccountingReportType type,
            DownloadFilter filter,
            String subtitle,
            String periodLabel,
            List<ReportSection> sections,
            ReportTotals totals,
            List<ExceptionItem> alerts) {

        return FinancialStatementReport.builder()
                .reportKey(type.getKey())
                .title(type.getTitle())
                .subtitle(periodLabel != null ? periodLabel : subtitle)
                .organizationName(resolveOrgName(organizationId))
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .sections(sections)
                .totals(totals)
                .validationAlerts(alerts != null ? alerts : List.of())
                .build();
    }

    private ReportSection section(String key, String label, List<ReportLine> lines, Double total) {
        return ReportSection.builder()
                .key(key)
                .label(label)
                .lines(lines)
                .sectionTotal(total)
                .formattedSectionTotal(total != null ? ReportingFormatUtil.compact(total) : null)
                .build();
    }

    private List<ExceptionItem> trialBalanceAlerts(double totalDebit, double totalCredit) {
        if (Math.abs(totalDebit - totalCredit) <= 0.01) {
            return List.of();
        }
        return List.of(ExceptionItem.builder()
                .key("trialImbalance")
                .severity(Severity.CRITICAL)
                .title("Trial balance out of balance")
                .description("Total debits do not equal total credits for the period.")
                .value(ReportingFormatUtil.round2(totalDebit - totalCredit))
                .formattedValue(ReportingFormatUtil.compact(totalDebit - totalCredit))
                .colorHint(ColorHint.DANGER)
                .build());
    }

    private LocalDateTime resolveAsOfDate(DownloadFilter filter) {
        if (filter != null && filter.getAsOfDate() != null && !filter.getAsOfDate().isBlank()) {
            return Utility.getEndOfDay(filter.getAsOfDate());
        }
        DateRange range = ReportingDateUtil.resolveRange(filter);
        return range.getEnd();
    }

    private String rangeSubtitle(DateRange range) {
        return range.getStart().toLocalDate() + " to " + range.getEnd().toLocalDate();
    }

    private String formatAsOf(LocalDateTime asOf) {
        return asOf.toLocalDate().toString();
    }

    private String resolveOrgName(long organizationId) {
        Optional<Organization> org = organizationRepo.findById(organizationId);
        return org.map(Organization::getName).orElse(null);
    }
}
