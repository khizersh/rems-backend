package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.analytics.reporting.admin.query.AdminAccountingReportRepository;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountCategorySummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountGroupSummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountSummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountTypeSummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.MonthlyAmountProjection;
import com.rem.backend.analytics.reporting.shared.dto.ChartData;
import com.rem.backend.analytics.reporting.shared.dto.ChartSeries;
import com.rem.backend.analytics.reporting.shared.dto.GroupedSummary;
import com.rem.backend.analytics.reporting.shared.dto.KpiCard;
import com.rem.backend.analytics.reporting.shared.dto.LabeledValue;
import com.rem.backend.analytics.reporting.shared.dto.ReportingView;
import com.rem.backend.analytics.reporting.shared.dto.TableColumn;
import com.rem.backend.analytics.reporting.shared.dto.TableData;
import com.rem.backend.analytics.reporting.shared.enums.ChartType;
import com.rem.backend.analytics.reporting.shared.enums.ColorHint;
import com.rem.backend.analytics.reporting.shared.enums.ValueType;
import com.rem.backend.analytics.reporting.shared.filters.ReportingFilter;
import com.rem.backend.analytics.reporting.shared.util.DateRange;
import com.rem.backend.analytics.reporting.shared.util.ReportingDateUtil;
import com.rem.backend.analytics.reporting.shared.util.ReportingFactory;
import com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rem.backend.analytics.reporting.shared.util.ReportingFormatUtil.nz;

/**
 * Admin financial reporting, derived entirely from the accounting module (POSTED journals
 * walked up the chart-of-account hierarchy). No financial figure is recomputed from business
 * tables here &mdash; accounting is the single source of truth.
 */
@Service
@AllArgsConstructor
public class AdminFinancialReportService {

    private final AdminAccountingReportRepository accountingRepo;

    // ---------------------------------------------------------------------
    // Financial overview (headline cards + by-type breakdown + net position)
    // ---------------------------------------------------------------------

    public ReportingView financialOverview(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<AccountTypeSummaryProjection> types =
                accountingRepo.summarizeByAccountType(organizationId, range.getStart(), range.getEnd());

        double assets = 0, liabilities = 0, income = 0, expense = 0, equity = 0;
        List<LabeledValue> typeItems = new ArrayList<>();
        for (AccountTypeSummaryProjection t : types) {
            double balance = AccountingReportSupport.naturalBalance(
                    t.getAccountType(), nz(t.getTotalDebit()), nz(t.getTotalCredit()));
            typeItems.add(ReportingFactory.currencyValue(
                    keyOf(t.getAccountType()), t.getAccountType(), balance,
                    AccountingReportSupport.colorForType(t.getAccountType())));

            String norm = t.getAccountType() == null ? "" : t.getAccountType().toUpperCase();
            if (norm.contains("ASSET")) {
                assets += balance;
            } else if (norm.contains("LIAB")) {
                liabilities += balance;
            } else if (norm.contains("INCOME") || norm.contains("REVENUE")) {
                income += balance;
            } else if (norm.contains("EXPENSE") || norm.contains("COST")) {
                expense += balance;
            } else if (norm.contains("EQUIT")) {
                equity += balance;
            }
        }

        double netProfit = income - expense;
        double netWorth = assets - liabilities;

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalAssets", "Total Assets", assets, "FaCoins", ColorHint.PRIMARY),
                ReportingFactory.currencyCard("totalLiabilities", "Total Liabilities", liabilities, "FaFileInvoiceDollar", ColorHint.WARNING),
                ReportingFactory.currencyCard("totalIncome", "Total Income", income, "FaArrowTrendUp", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("totalExpense", "Total Expense", expense, "FaReceipt", ColorHint.DANGER),
                ReportingFactory.currencyCard("netProfit", "Net Profit", netProfit, "FaScaleBalanced",
                        netProfit >= 0 ? ColorHint.SUCCESS : ColorHint.DANGER),
                ReportingFactory.currencyCard("netWorth", "Net Worth (Equity)", netWorth, "FaBuildingColumns",
                        netWorth >= 0 ? ColorHint.INFO : ColorHint.DANGER)
        );

        GroupedSummary byType = GroupedSummary.builder()
                .key("byAccountType")
                .label("Balances by Account Type")
                .total(ReportingFormatUtil.round2(assets + liabilities + income + expense + equity))
                .items(typeItems)
                .build();

        // Asset category drill-down (CURRENT ASSET vs FIXED ASSET)
        double currentAssets = categoryBalance(organizationId, range, "ASSET", "CURRENT");
        double fixedAssets   = categoryBalance(organizationId, range, "ASSET", "FIXED");
        GroupedSummary assetBreakdown = GroupedSummary.builder()
                .key("assetByCategory")
                .label("Assets (by Category)")
                .total(ReportingFormatUtil.round2(assets))
                .formattedTotal(ReportingFormatUtil.compact(assets))
                .items(List.of(
                        ReportingFactory.currencyValue("currentAssets", "Current Assets", currentAssets, ColorHint.SUCCESS),
                        ReportingFactory.currencyValue("fixedAssets",   "Fixed Assets",   fixedAssets,   ColorHint.INFO)))
                .build();

        // Liability category drill-down (CURRENT LIABILITY vs LONG TERM LIABILITY)
        double currentLiabilities  = categoryBalance(organizationId, range, "LIABILITY", "CURRENT");
        double longTermLiabilities = categoryBalance(organizationId, range, "LIABILITY", "LONG TERM");
        GroupedSummary liabilityBreakdown = GroupedSummary.builder()
                .key("liabilityByCategory")
                .label("Liabilities (by Category)")
                .total(ReportingFormatUtil.round2(liabilities))
                .formattedTotal(ReportingFormatUtil.compact(liabilities))
                .items(List.of(
                        ReportingFactory.currencyValue("currentLiabilities",  "Current Liabilities",
                                currentLiabilities,  ColorHint.DANGER),
                        ReportingFactory.currencyValue("longTermLiabilities", "Long-Term Liabilities",
                                longTermLiabilities, ColorHint.WARNING)))
                .build();

        // Income category drill-down (OPERATING INCOME vs OTHER INCOME)
        double operatingIncome = categoryBalance(organizationId, range, "INCOME", "OPERATING");
        double otherIncome     = categoryBalance(organizationId, range, "INCOME", "OTHER");
        GroupedSummary incomeBreakdown = GroupedSummary.builder()
                .key("incomeByCategory")
                .label("Income (by Category)")
                .total(ReportingFormatUtil.round2(income))
                .formattedTotal(ReportingFormatUtil.compact(income))
                .items(List.of(
                        ReportingFactory.currencyValue("operatingIncome", "Operating Income", operatingIncome, ColorHint.SUCCESS),
                        ReportingFactory.currencyValue("otherIncome",     "Other Income",     otherIncome,     ColorHint.INFO)))
                .build();

        // Expense category drill-down (DIRECT EXPENSE vs INDIRECT EXPENSE)
        double directExpense   = categoryBalance(organizationId, range, "EXPENSE", "DIRECT");
        double indirectExpense = categoryBalance(organizationId, range, "EXPENSE", "INDIRECT");
        GroupedSummary expenseBreakdown = GroupedSummary.builder()
                .key("expenseByCategory")
                .label("Expenses (by Category)")
                .total(ReportingFormatUtil.round2(expense))
                .formattedTotal(ReportingFormatUtil.compact(expense))
                .items(List.of(
                        ReportingFactory.currencyValue("directExpense",   "Direct Expense",   directExpense,   ColorHint.DANGER),
                        ReportingFactory.currencyValue("indirectExpense", "Indirect Expense", indirectExpense, ColorHint.WARNING)))
                .build();

        return ReportingView.builder()
                .key("financial-overview")
                .title("Financial Overview")
                .subtitle("Accounting-derived summary across all 5 account types")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of(byType, assetBreakdown, liabilityBreakdown, incomeBreakdown, expenseBreakdown))
                .charts(List.of(revenueVsCollectionChart(organizationId, range)))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    public ReportingView financialOverviewByType(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<AccountTypeSummaryProjection> types =
                accountingRepo.summarizeByAccountType(organizationId, range.getStart(), range.getEnd());

        List<Map<String, Object>> rows = new ArrayList<>();
        List<LabeledValue> items = new ArrayList<>();
        for (AccountTypeSummaryProjection t : types) {
            double debit = nz(t.getTotalDebit());
            double credit = nz(t.getTotalCredit());
            double balance = AccountingReportSupport.naturalBalance(t.getAccountType(), debit, credit);
            rows.add(orderedRow(
                    "accountType", t.getAccountType(),
                    "totalDebit", ReportingFormatUtil.round2(debit),
                    "totalCredit", ReportingFormatUtil.round2(credit),
                    "balance", ReportingFormatUtil.round2(balance)));
            items.add(ReportingFactory.currencyValue(keyOf(t.getAccountType()), t.getAccountType(), balance,
                    AccountingReportSupport.colorForType(t.getAccountType())));
        }

        return aggregationView("financial-overview-by-type", "Financial Overview by Account Type",
                filter, items, debitCreditBalanceColumns("accountType", "Account Type"), rows);
    }

    public ReportingView financialOverviewByCategory(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<AccountCategorySummaryProjection> cats =
                accountingRepo.summarizeByAccountCategory(organizationId, range.getStart(), range.getEnd());

        // Group categories under their parent type for a hierarchical breakdown.
        Map<String, List<LabeledValue>> grouped = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccountCategorySummaryProjection c : cats) {
            double debit = nz(c.getTotalDebit());
            double credit = nz(c.getTotalCredit());
            double balance = AccountingReportSupport.naturalBalance(c.getAccountType(), debit, credit);
            grouped.computeIfAbsent(c.getAccountType(), k -> new ArrayList<>())
                    .add(ReportingFactory.currencyValue(keyOf(c.getAccountCategory()), c.getAccountCategory(),
                            balance, AccountingReportSupport.colorForType(c.getAccountType())));
            rows.add(orderedRow(
                    "accountType", c.getAccountType(),
                    "accountCategory", c.getAccountCategory(),
                    "totalDebit", ReportingFormatUtil.round2(debit),
                    "totalCredit", ReportingFormatUtil.round2(credit),
                    "balance", ReportingFormatUtil.round2(balance)));
        }

        List<GroupedSummary> breakdowns = new ArrayList<>();
        for (Map.Entry<String, List<LabeledValue>> e : grouped.entrySet()) {
            double total = e.getValue().stream().mapToDouble(LabeledValue::getValue).sum();
            breakdowns.add(GroupedSummary.builder()
                    .key(keyOf(e.getKey()))
                    .label(e.getKey())
                    .total(ReportingFormatUtil.round2(total))
                    .formattedTotal(ReportingFormatUtil.compact(total))
                    .items(e.getValue())
                    .build());
        }

        List<TableColumn> columns = new ArrayList<>();
        columns.add(col("accountType", "Account Type", ValueType.TEXT));
        columns.add(col("accountCategory", "Category", ValueType.TEXT));
        columns.addAll(debitCreditBalanceColumns(null, null));

        return ReportingView.builder()
                .key("financial-overview-by-category")
                .title("Financial Overview by Account Category")
                .subtitle("Accounting-derived summary")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(List.of())
                .breakdowns(breakdowns)
                .charts(List.of())
                .tables(List.of(TableData.builder()
                        .key("categoryBalances").title("Category Balances")
                        .columns(columns).rows(rows).build()))
                .alerts(List.of())
                .build();
    }

    public ReportingView financialOverviewByAccount(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<AccountSummaryProjection> accounts =
                accountingRepo.summarizeByAccount(organizationId, range.getStart(), range.getEnd());

        String typeFilter = filter == null ? null : filter.getAccountType();
        String categoryFilter = filter == null ? null : filter.getAccountCategory();
        String codeFilter = filter == null ? null : filter.getAccountCode();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccountSummaryProjection a : accounts) {
            if (!matches(typeFilter, a.getAccountType())
                    || !matches(categoryFilter, a.getAccountCategory())
                    || !matches(codeFilter, a.getAccountCode())) {
                continue;
            }
            double debit = nz(a.getTotalDebit());
            double credit = nz(a.getTotalCredit());
            double balance = AccountingReportSupport.naturalBalance(a.getAccountType(), debit, credit);
            rows.add(orderedRow(
                    "accountType", a.getAccountType(),
                    "accountCategory", a.getAccountCategory(),
                    "accountCode", a.getAccountCode(),
                    "accountName", a.getAccountName(),
                    "totalDebit", ReportingFormatUtil.round2(debit),
                    "totalCredit", ReportingFormatUtil.round2(credit),
                    "balance", ReportingFormatUtil.round2(balance)));
        }

        List<TableColumn> columns = List.of(
                col("accountType", "Type", ValueType.TEXT),
                col("accountCategory", "Category", ValueType.TEXT),
                col("accountCode", "Account Code", ValueType.TEXT),
                col("accountName", "Account", ValueType.TEXT),
                col("totalDebit", "Debit", ValueType.CURRENCY),
                col("totalCredit", "Credit", ValueType.CURRENCY),
                col("balance", "Balance", ValueType.CURRENCY));

        return ReportingView.builder()
                .key("financial-overview-by-account")
                .title("Financial Overview by Chart of Account")
                .subtitle("Accounting-derived summary")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(List.of())
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of(TableData.builder()
                        .key("accountBalances").title("Account Balances")
                        .columns(columns).rows(rows).build()))
                .alerts(List.of())
                .build();
    }

    // ---------------------------------------------------------------------
    // Revenue / expense / receivable / payable summaries
    // ---------------------------------------------------------------------

    /**
     * Revenue summary using the COMPLETE INCOME type hierarchy.
     * <p>
     * Hierarchy: INCOME (account_type) → OPERATING INCOME / OTHER INCOME (account_category)
     * → Booking-Revenue account_group → individual COA codes (INC-BOOKING-001 etc.).
     * <p>
     * KPI cards: total income + OPERATING vs OTHER category split + individual code breakdown.
     * Grouped summary shows every income group under its category.
     * Trend chart covers the full INCOME type (not just booking) for a complete picture.
     */
    public ReportingView revenueSummary(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        // Total income across the FULL hierarchy
        double totalIncome     = typeBalance(organizationId, range, "INCOME");
        double operatingIncome = categoryBalance(organizationId, range, "INCOME", "OPERATING");
        double otherIncome     = categoryBalance(organizationId, range, "INCOME", "OTHER");

        // Specific income codes that are provisioned in the COA
        double bookingIncome      = codeBalance(organizationId, range, JournalUtilities.BOOKING_REVENUE);       // INC-BOOKING-001
        double cancellationIncome = codeBalance(organizationId, range, JournalUtilities.CANCELLATION_REVENUE);  // INC-CANCEL-001
        double scrapIncome        = codeBalance(organizationId, range, JournalUtilities.SCRAP_INCOME_ACCOUNT);  // INC-SCRAP-001

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalIncome",        "Total Income",        totalIncome,        "FaArrowTrendUp",     ColorHint.SUCCESS),
                ReportingFactory.currencyCard("operatingIncome",    "Operating Income",    operatingIncome,    "FaBuildingColumns",  ColorHint.SUCCESS),
                ReportingFactory.currencyCard("otherIncome",        "Other Income",        otherIncome,        "FaCirclePlus",       ColorHint.INFO),
                ReportingFactory.currencyCard("bookingIncome",      "Booking Income",      bookingIncome,      "FaHouseCircleCheck", ColorHint.SUCCESS),
                ReportingFactory.currencyCard("cancellationIncome", "Cancellation Income", cancellationIncome, "FaRotateLeft",       ColorHint.INFO),
                ReportingFactory.currencyCard("scrapIncome",        "Scrap Income",        scrapIncome,        "FaRecycle",          ColorHint.NEUTRAL)
        );

        // Group-level breakdown: INCOME type → category → group
        List<AccountGroupSummaryProjection> incomeGroups =
                accountingRepo.summarizeByAccountGroupForType(organizationId, "INCOME", range.getStart(), range.getEnd());

        Map<String, List<LabeledValue>> byCat = new LinkedHashMap<>();
        for (AccountGroupSummaryProjection g : incomeGroups) {
            double balance = AccountingReportSupport.naturalBalance(
                    g.getAccountType(), nz(g.getTotalDebit()), nz(g.getTotalCredit()));
            byCat.computeIfAbsent(g.getAccountCategory(), k -> new ArrayList<>())
                    .add(ReportingFactory.currencyValue(
                            keyOf(g.getAccountGroup()), g.getAccountGroup(), balance,
                            AccountingReportSupport.colorForCategory(g.getAccountCategory())));
        }

        List<GroupedSummary> breakdowns = new ArrayList<>();
        for (Map.Entry<String, List<LabeledValue>> e : byCat.entrySet()) {
            double catTotal = e.getValue().stream().mapToDouble(LabeledValue::getValue).sum();
            breakdowns.add(GroupedSummary.builder()
                    .key(keyOf(e.getKey()))
                    .label(e.getKey())
                    .total(ReportingFormatUtil.round2(catTotal))
                    .formattedTotal(ReportingFormatUtil.compact(catTotal))
                    .items(e.getValue())
                    .build());
        }

        // Monthly trend across full INCOME type
        ChartData trend = monthlyChartFromType("monthlyRevenue", "Monthly Income Trend", "Income",
                accountingRepo.monthlyByAccountType(organizationId, "INCOME", range.getStart(), range.getEnd()),
                range, true, ColorHint.SUCCESS);

        return ReportingView.builder()
                .key("financial-revenue-summary")
                .title("Revenue Summary")
                .subtitle("Income hierarchy: Operating + Other, broken down by account group")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(breakdowns)
                .charts(List.of(trend))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    /**
     * Expense summary using the COMPLETE EXPENSE type hierarchy.
     * <p>
     * Hierarchy: EXPENSE (account_type) → DIRECT EXPENSE / INDIRECT EXPENSE (account_category)
     * → individual account_groups → COA accounts.
     * <p>
     * Note: EXP-CONST-001 and EXP-MISC-001 are not provisioned in this system — construction
     * costs are capitalised into ASSET inventory accounts (Projects-Inventory group). Only codes
     * that actually exist in the COA are used for individual KPIs.
     */
    public ReportingView expenseSummary(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        // Total expense across the FULL hierarchy (DIRECT + INDIRECT categories)
        double totalExpense    = typeBalance(organizationId, range, "EXPENSE");
        double directExpense   = categoryBalance(organizationId, range, "EXPENSE", "DIRECT");
        double indirectExpense = categoryBalance(organizationId, range, "EXPENSE", "INDIRECT");

        // System-defined expense accounts that are provisioned in the COA
        double salaryExpense     = codeBalance(organizationId, range, JournalUtilities.SALARY_EXPENSE);     // EXP-SALARY-001
        double adjustmentExpense = codeBalance(organizationId, range, JournalUtilities.ADJUSTMENT_EXPENSE); // EXP-ADJUSTMENT-001

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalExpense",      "Total Expense",      totalExpense,      "FaReceipt",      ColorHint.DANGER),
                ReportingFactory.currencyCard("directExpense",     "Direct Expense",     directExpense,     "FaTrowelBricks", ColorHint.WARNING),
                ReportingFactory.currencyCard("indirectExpense",   "Indirect Expense",   indirectExpense,   "FaLayerGroup",   ColorHint.INFO),
                ReportingFactory.currencyCard("salaryExpense",     "Salary Expense",     salaryExpense,     "FaUsers",        ColorHint.INFO),
                ReportingFactory.currencyCard("adjustmentExpense", "Adjustment Expense", adjustmentExpense, "FaSliders",      ColorHint.NEUTRAL)
        );

        // Group-level breakdown: EXPENSE type → category → group
        List<AccountGroupSummaryProjection> expenseGroups =
                accountingRepo.summarizeByAccountGroupForType(organizationId, "EXPENSE", range.getStart(), range.getEnd());

        Map<String, List<LabeledValue>> byCat = new LinkedHashMap<>();
        for (AccountGroupSummaryProjection g : expenseGroups) {
            double balance = AccountingReportSupport.naturalBalance(
                    g.getAccountType(), nz(g.getTotalDebit()), nz(g.getTotalCredit()));
            byCat.computeIfAbsent(g.getAccountCategory(), k -> new ArrayList<>())
                    .add(ReportingFactory.currencyValue(
                            keyOf(g.getAccountGroup()), g.getAccountGroup(), balance,
                            AccountingReportSupport.colorForCategory(g.getAccountCategory())));
        }

        List<GroupedSummary> breakdowns = new ArrayList<>();
        for (Map.Entry<String, List<LabeledValue>> e : byCat.entrySet()) {
            double catTotal = e.getValue().stream().mapToDouble(LabeledValue::getValue).sum();
            breakdowns.add(GroupedSummary.builder()
                    .key(keyOf(e.getKey()))
                    .label(e.getKey())
                    .total(ReportingFormatUtil.round2(catTotal))
                    .formattedTotal(ReportingFormatUtil.compact(catTotal))
                    .items(e.getValue())
                    .build());
        }

        // Monthly trend across full EXPENSE type (not a single non-existent code)
        ChartData trend = monthlyChartFromType("monthlyExpense", "Monthly Expense Trend", "Expense",
                accountingRepo.monthlyByAccountType(organizationId, "EXPENSE", range.getStart(), range.getEnd()),
                range, false, ColorHint.DANGER);

        return ReportingView.builder()
                .key("financial-expense-summary")
                .title("Expense Summary")
                .subtitle("Expense hierarchy: Direct + Indirect, broken down by account group")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(breakdowns)
                .charts(List.of(trend))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    /**
     * Receivable summary using the full ASSET hierarchy.
     * <p>
     * Hierarchy path: ASSET (account_type) → current asset categories →
     * receivable account_groups → individual COA codes.
     * Currently the system has one primary receivable account (Customer Receivable),
     * but the breakdown shows all ASSET groups so the admin sees the complete picture.
     */
    public ReportingView receivableSummary(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        // Total customer receivable (primary KPI)
        double customerReceivable = codeBalance(organizationId, range, JournalUtilities.CUSTOMER_RECEIVABLE);

        // Full ASSET type balance (total assets for context)
        double totalAssets = typeBalance(organizationId, range, "ASSET");

        // Group-level breakdown of all ASSET accounts so the admin sees every asset group
        List<AccountGroupSummaryProjection> assetGroups =
                accountingRepo.summarizeByAccountGroupForType(organizationId, "ASSET", range.getStart(), range.getEnd());

        // Build category → group hierarchy
        Map<String, List<LabeledValue>> byCat = new LinkedHashMap<>();
        for (AccountGroupSummaryProjection g : assetGroups) {
            double balance = AccountingReportSupport.naturalBalance(
                    g.getAccountType(), nz(g.getTotalDebit()), nz(g.getTotalCredit()));
            byCat.computeIfAbsent(g.getAccountCategory(), k -> new ArrayList<>())
                    .add(ReportingFactory.currencyValue(
                            keyOf(g.getAccountGroup()), g.getAccountGroup(), balance,
                            AccountingReportSupport.colorForCategory(g.getAccountCategory())));
        }

        List<GroupedSummary> breakdowns = new ArrayList<>();
        for (Map.Entry<String, List<LabeledValue>> e : byCat.entrySet()) {
            double catTotal = e.getValue().stream().mapToDouble(LabeledValue::getValue).sum();
            breakdowns.add(GroupedSummary.builder()
                    .key(keyOf(e.getKey()))
                    .label(e.getKey())
                    .total(ReportingFormatUtil.round2(catTotal))
                    .formattedTotal(ReportingFormatUtil.compact(catTotal))
                    .items(e.getValue())
                    .build());
        }

        // KPI cards
        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("customerReceivable", "Customer Receivable",
                        customerReceivable, "FaHandHoldingDollar", ColorHint.WARNING),
                ReportingFactory.currencyCard("totalAssets", "Total Assets",
                        totalAssets, "FaCoins", ColorHint.PRIMARY)
        );

        // Monthly collections trend (credit side of the receivable account = cash received)
        ChartData collections = monthlyChart("monthlyCollections", "Monthly Collections", "Collections",
                accountingRepo.monthlyByAccountCode(organizationId, JournalUtilities.CUSTOMER_RECEIVABLE,
                        range.getStart(), range.getEnd()), range, /*useCredit=*/ true, ColorHint.SUCCESS);

        return ReportingView.builder()
                .key("financial-receivable-summary")
                .title("Receivable Summary")
                .subtitle("Customer receivable & full asset hierarchy")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(breakdowns)
                .charts(List.of(collections))
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    /**
     * Payable summary using the COMPLETE LIABILITY hierarchy.
     * <p>
     * Hierarchy: LIABILITY (account_type) → CURRENT LIABILITY / LONG TERM LIABILITY
     * (account_category) → Accounts Payable / Booking Liability / … (account_group)
     * → individual COA accounts (journal_detail_entry).
     * <p>
     * KPI cards show total payables + split by current/long-term.
     * Breakdowns show each liability category with its account groups as line items.
     * Table shows every individual liability account.
     */
    public ReportingView payableSummary(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);

        // ---- Total payables = ALL accounts under LIABILITY type ----------------
        double totalPayables = typeBalance(organizationId, range, "LIABILITY");
        double currentLiabilities  = categoryBalance(organizationId, range, "LIABILITY", "CURRENT");
        double longTermLiabilities = categoryBalance(organizationId, range, "LIABILITY", "LONG TERM");

        // ---- Group-level breakdown: LIABILITY → category → group ---------------
        List<AccountGroupSummaryProjection> liabilityGroups =
                accountingRepo.summarizeByAccountGroupForType(
                        organizationId, "LIABILITY", range.getStart(), range.getEnd());

        Map<String, List<LabeledValue>> byCat = new LinkedHashMap<>();
        for (AccountGroupSummaryProjection g : liabilityGroups) {
            double balance = AccountingReportSupport.naturalBalance(
                    g.getAccountType(), nz(g.getTotalDebit()), nz(g.getTotalCredit()));
            byCat.computeIfAbsent(g.getAccountCategory(), k -> new ArrayList<>())
                    .add(ReportingFactory.currencyValue(
                            keyOf(g.getAccountGroup()), g.getAccountGroup(), balance,
                            AccountingReportSupport.colorForCategory(g.getAccountCategory())));
        }

        List<GroupedSummary> breakdowns = new ArrayList<>();
        for (Map.Entry<String, List<LabeledValue>> e : byCat.entrySet()) {
            double catTotal = e.getValue().stream().mapToDouble(LabeledValue::getValue).sum();
            breakdowns.add(GroupedSummary.builder()
                    .key(keyOf(e.getKey()))
                    .label(e.getKey())
                    .total(ReportingFormatUtil.round2(catTotal))
                    .formattedTotal(ReportingFormatUtil.compact(catTotal))
                    .items(e.getValue())
                    .build());
        }

        // ---- Individual account detail table (all LIABILITY COA accounts) ------
        List<AccountSummaryProjection> accounts =
                accountingRepo.summarizeByAccount(organizationId, range.getStart(), range.getEnd());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccountSummaryProjection a : accounts) {
            String typeNorm = a.getAccountType() == null ? "" : a.getAccountType().toUpperCase();
            if (!typeNorm.contains("LIAB")) {
                continue;
            }
            double debit  = nz(a.getTotalDebit());
            double credit = nz(a.getTotalCredit());
            double balance = AccountingReportSupport.naturalBalance(a.getAccountType(), debit, credit);
            rows.add(orderedRow(
                    "accountCategory", a.getAccountCategory(),
                    "accountCode",     a.getAccountCode(),
                    "accountName",     a.getAccountName(),
                    "totalDebit",      ReportingFormatUtil.round2(debit),
                    "totalCredit",     ReportingFormatUtil.round2(credit),
                    "balance",         ReportingFormatUtil.round2(balance)));
        }

        List<TableColumn> columns = List.of(
                col("accountCategory", "Category",     ValueType.TEXT),
                col("accountCode",     "Code",         ValueType.TEXT),
                col("accountName",     "Account",      ValueType.TEXT),
                col("totalDebit",      "Debit",        ValueType.CURRENCY),
                col("totalCredit",     "Credit",       ValueType.CURRENCY),
                col("balance",         "Balance",      ValueType.CURRENCY));

        // ---- Monthly total-liability trend chart (all accounts, credit = new payables added)
        ChartData trend = monthlyChartFromType("monthlyPayable", "Monthly Liability Movement", "Payable",
                accountingRepo.monthlyByAccountType(
                        organizationId, "LIABILITY", range.getStart(), range.getEnd()),
                range, /*useCredit=*/ true, ColorHint.DANGER);

        // ---- KPI cards ---------------------------------------------------------
        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalPayables",       "Total Payables",
                        totalPayables,       "FaFileInvoiceDollar", ColorHint.DANGER),
                ReportingFactory.currencyCard("currentLiabilities",  "Current Liabilities",
                        currentLiabilities,  "FaHourglassHalf",     ColorHint.WARNING),
                ReportingFactory.currencyCard("longTermLiabilities", "Long-Term Liabilities",
                        longTermLiabilities, "FaCalendarXmark",     ColorHint.NEUTRAL)
        );

        return ReportingView.builder()
                .key("financial-payable-summary")
                .title("Payable Summary")
                .subtitle("Full liability hierarchy — current & long-term")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(breakdowns)
                .charts(List.of(trend))
                .tables(List.of(TableData.builder()
                        .key("liabilityAccounts")
                        .title("Liability Account Detail")
                        .columns(columns)
                        .rows(rows)
                        .build()))
                .alerts(List.of())
                .build();
    }

    public ReportingView accountingTrialSummary(long organizationId, ReportingFilter filter) {
        DateRange range = ReportingDateUtil.resolveRange(filter);
        List<AccountSummaryProjection> accounts =
                accountingRepo.summarizeByAccount(organizationId, range.getStart(), range.getEnd());

        double totalDebit = 0, totalCredit = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccountSummaryProjection a : accounts) {
            double debit = nz(a.getTotalDebit());
            double credit = nz(a.getTotalCredit());
            totalDebit += debit;
            totalCredit += credit;
            rows.add(orderedRow(
                    "accountCode", a.getAccountCode(),
                    "accountName", a.getAccountName(),
                    "accountType", a.getAccountType(),
                    "totalDebit", ReportingFormatUtil.round2(debit),
                    "totalCredit", ReportingFormatUtil.round2(credit)));
        }

        List<KpiCard> cards = List.of(
                ReportingFactory.currencyCard("totalDebit", "Total Debit", totalDebit, "FaArrowRightToBracket", ColorHint.PRIMARY),
                ReportingFactory.currencyCard("totalCredit", "Total Credit", totalCredit, "FaArrowRightFromBracket", ColorHint.INFO),
                ReportingFactory.currencyCard("difference", "Debit - Credit", totalDebit - totalCredit, "FaScaleBalanced",
                        Math.abs(totalDebit - totalCredit) < 0.01 ? ColorHint.SUCCESS : ColorHint.DANGER)
        );

        TableData table = TableData.builder()
                .key("trialBalance").title("Trial Balance Summary")
                .columns(List.of(
                        col("accountCode", "Code", ValueType.TEXT),
                        col("accountName", "Account", ValueType.TEXT),
                        col("accountType", "Type", ValueType.TEXT),
                        col("totalDebit", "Debit", ValueType.CURRENCY),
                        col("totalCredit", "Credit", ValueType.CURRENCY)))
                .rows(rows).build();

        return ReportingView.builder()
                .key("accounting-trial-summary")
                .title("Accounting Trial Summary")
                .subtitle("Posted journal totals by account")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(List.of())
                .tables(List.of(table))
                .alerts(List.of())
                .build();
    }

    // ---------------------------------------------------------------------
    // Shared building blocks reused by the dashboard service
    // ---------------------------------------------------------------------

    /** Natural balance for a whole account type (e.g. "INCOME"), org + range scoped. */
    public double typeBalance(long organizationId, DateRange range, String typeKeyword) {
        List<AccountTypeSummaryProjection> types =
                accountingRepo.summarizeByAccountType(organizationId, range.getStart(), range.getEnd());
        double sum = 0;
        for (AccountTypeSummaryProjection t : types) {
            String norm = t.getAccountType() == null ? "" : t.getAccountType().toUpperCase();
            if (norm.contains(typeKeyword.toUpperCase())) {
                sum += AccountingReportSupport.naturalBalance(
                        t.getAccountType(), nz(t.getTotalDebit()), nz(t.getTotalCredit()));
            }
        }
        return sum;
    }

    /** Natural balance for a single account code; 0 when the account has no postings. */
    public double codeBalance(long organizationId, DateRange range, String accountCode) {
        List<AccountSummaryProjection> list =
                accountingRepo.summarizeByAccountCode(organizationId, accountCode, range.getStart(), range.getEnd());
        if (list.isEmpty()) {
            return 0d;
        }
        AccountSummaryProjection a = list.get(0);
        return AccountingReportSupport.naturalBalance(a.getAccountType(), nz(a.getTotalDebit()), nz(a.getTotalCredit()));
    }

    /**
     * Natural balance summed across all account_categories whose name contains
     * {@code categoryKeyword} (case-insensitive) within the given account type.
     * <p>
     * Example: {@code categoryBalance(orgId, range, "LIABILITY", "CURRENT")} returns
     * the net balance of every CURRENT LIABILITY category account.
     */
    public double categoryBalance(long organizationId, DateRange range,
                                  String accountTypeName, String categoryKeyword) {
        List<AccountCategorySummaryProjection> cats =
                accountingRepo.summarizeByAccountCategory(organizationId, range.getStart(), range.getEnd());
        double sum = 0d;
        for (AccountCategorySummaryProjection c : cats) {
            String typeNorm = c.getAccountType()     == null ? "" : c.getAccountType().toUpperCase();
            String catNorm  = c.getAccountCategory() == null ? "" : c.getAccountCategory().toUpperCase();
            if (typeNorm.contains(accountTypeName.toUpperCase())
                    && catNorm.contains(categoryKeyword.toUpperCase())) {
                sum += AccountingReportSupport.naturalBalance(
                        c.getAccountType(), nz(c.getTotalDebit()), nz(c.getTotalCredit()));
            }
        }
        return sum;
    }

    /**
     * Natural balance summed across all account_groups whose name contains
     * {@code groupKeyword} within the given account type.
     */
    public double groupBalance(long organizationId, DateRange range,
                               String accountTypeName, String groupKeyword) {
        List<AccountGroupSummaryProjection> groups =
                accountingRepo.summarizeByAccountGroupForType(
                        organizationId, accountTypeName, range.getStart(), range.getEnd());
        double sum = 0d;
        for (AccountGroupSummaryProjection g : groups) {
            String grpNorm = g.getAccountGroup() == null ? "" : g.getAccountGroup().toUpperCase();
            if (grpNorm.contains(groupKeyword.toUpperCase())) {
                sum += AccountingReportSupport.naturalBalance(
                        g.getAccountType(), nz(g.getTotalDebit()), nz(g.getTotalCredit()));
            }
        }
        return sum;
    }

    public ChartData revenueVsCollectionChart(long organizationId, DateRange range) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        List<Object> revenue = alignMonthly(
                accountingRepo.monthlyByAccountCode(organizationId, JournalUtilities.BOOKING_REVENUE,
                        range.getStart(), range.getEnd()), buckets, true);
        List<Object> collections = alignMonthly(
                accountingRepo.monthlyByAccountCode(organizationId, JournalUtilities.CUSTOMER_RECEIVABLE,
                        range.getStart(), range.getEnd()), buckets, true);
        return ChartData.builder()
                .key("revenueVsCollection")
                .title("Revenue vs Collection")
                .type(ChartType.LINE)
                .labels(monthLabels(buckets))
                .series(List.of(
                        ChartSeries.builder().name("Booking Income").data(revenue).colorHint(ColorHint.SUCCESS).build(),
                        ChartSeries.builder().name("Collections").data(collections).colorHint(ColorHint.PRIMARY).build()))
                .build();
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private ChartData monthlyChart(String key, String title, String seriesName,
                                   List<MonthlyAmountProjection> data, DateRange range,
                                   boolean useCredit, ColorHint color) {
        List<YearMonth> buckets = ReportingDateUtil.monthBuckets(range);
        List<Object> values = alignMonthly(data, buckets, useCredit);
        return ChartData.builder()
                .key(key).title(title).type(ChartType.BAR)
                .labels(monthLabels(buckets))
                .series(List.of(ChartSeries.builder().name(seriesName).data(values).colorHint(color).build()))
                .build();
    }

    /** Same as {@link #monthlyChart} but accepts data from the account-type-level monthly query. */
    private ChartData monthlyChartFromType(String key, String title, String seriesName,
                                           List<MonthlyAmountProjection> data, DateRange range,
                                           boolean useCredit, ColorHint color) {
        return monthlyChart(key, title, seriesName, data, range, useCredit, color);
    }

    /** Aligns sparse monthly projections to the full bucket list, filling gaps with 0. */
    private List<Object> alignMonthly(List<MonthlyAmountProjection> data, List<YearMonth> buckets, boolean useCredit) {
        Map<YearMonth, Double> byMonth = new LinkedHashMap<>();
        for (MonthlyAmountProjection p : data) {
            if (p.getYr() == null || p.getMonthNo() == null) {
                continue;
            }
            double v = useCredit ? nz(p.getTotalCredit()) : nz(p.getTotalDebit());
            byMonth.put(YearMonth.of(p.getYr(), p.getMonthNo()), ReportingFormatUtil.round2(v));
        }
        List<Object> values = new ArrayList<>();
        for (YearMonth ym : buckets) {
            values.add(byMonth.getOrDefault(ym, 0d));
        }
        return values;
    }

    private List<String> monthLabels(List<YearMonth> buckets) {
        List<String> labels = new ArrayList<>();
        for (YearMonth ym : buckets) {
            labels.add(ReportingDateUtil.monthLabel(ym));
        }
        return labels;
    }

    private ReportingView aggregationView(String key, String title, ReportingFilter filter,
                                          List<LabeledValue> items, List<TableColumn> columns,
                                          List<Map<String, Object>> rows) {
        GroupedSummary summary = GroupedSummary.builder()
                .key("balances").label("Balances")
                .total(ReportingFormatUtil.round2(items.stream().mapToDouble(LabeledValue::getValue).sum()))
                .items(items).build();
        return ReportingView.builder()
                .key(key).title(title).subtitle("Accounting-derived summary")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(List.of())
                .breakdowns(List.of(summary))
                .charts(List.of())
                .tables(List.of(TableData.builder().key("detail").title("Detail").columns(columns).rows(rows).build()))
                .alerts(List.of())
                .build();
    }

    private ReportingView simpleView(String key, String title, ReportingFilter filter,
                                     List<KpiCard> cards, List<ChartData> charts) {
        return ReportingView.builder()
                .key(key).title(title).subtitle("Accounting-derived summary")
                .generatedAt(LocalDateTime.now())
                .filtersApplied(ReportingFactory.appliedFilters(filter))
                .summaryCards(cards)
                .breakdowns(List.of())
                .charts(charts)
                .tables(List.of())
                .alerts(List.of())
                .build();
    }

    private List<TableColumn> debitCreditBalanceColumns(String firstKey, String firstLabel) {
        List<TableColumn> cols = new ArrayList<>();
        if (firstKey != null) {
            cols.add(col(firstKey, firstLabel, ValueType.TEXT));
        }
        cols.add(col("totalDebit", "Debit", ValueType.CURRENCY));
        cols.add(col("totalCredit", "Credit", ValueType.CURRENCY));
        cols.add(col("balance", "Balance", ValueType.CURRENCY));
        return cols;
    }

    private static TableColumn col(String key, String label, ValueType type) {
        return TableColumn.builder().key(key).label(label).type(type).build();
    }

    private static Map<String, Object> orderedRow(Object... kv) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            row.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return row;
    }

    private static boolean matches(String filterValue, String actual) {
        if (filterValue == null || filterValue.isBlank()) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(filterValue);
    }

    private static String keyOf(String label) {
        if (label == null) {
            return "unknown";
        }
        return label.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
