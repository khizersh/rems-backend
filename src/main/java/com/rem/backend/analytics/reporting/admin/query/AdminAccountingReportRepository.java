package com.rem.backend.analytics.reporting.admin.query;

import com.rem.backend.accountingmanagement.entity.JournalDetailEntry;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountCategorySummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountGroupSummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountSummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountTypeSummaryProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.MonthlyAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only reporting repository over the double-entry ledger.
 * <p>
 * Every figure is derived from POSTED journal lines walked up the full 5-level
 * COA hierarchy: journal_detail_entry → chart_of_account → account_group →
 * account_category → account_type.
 * <p>
 * Queries are intentionally isolated here (separate from accountingmanagement
 * repositories) so reporting aggregations never interfere with transactional code.
 * Only {@code status = 'POSTED'} entries are included; everything is scoped to a
 * single organization for tenant safety.
 */
@Repository
public interface AdminAccountingReportRepository extends JpaRepository<JournalDetailEntry, Long> {

    // =========================================================================
    // LEVEL 1 — Account Type (ASSET / LIABILITY / INCOME / EXPENSE / EQUITY)
    // =========================================================================

    /** Posted debit/credit totals grouped by account_type for the org + date range. */
    @Query(value = """
            SELECT at.name AS accountType,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY at.name
            ORDER BY at.name
            """, nativeQuery = true)
    List<AccountTypeSummaryProjection> summarizeByAccountType(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // LEVEL 2 — Account Category (CURRENT ASSET / CURRENT LIABILITY / etc.)
    // =========================================================================

    /** Posted debit/credit totals grouped by account_category for the org + date range. */
    @Query(value = """
            SELECT at.name AS accountType,
                   ac.name AS accountCategory,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY at.name, ac.name
            ORDER BY at.name, ac.name
            """, nativeQuery = true)
    List<AccountCategorySummaryProjection> summarizeByAccountCategory(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // LEVEL 3 — Account Group (Accounts Payable / Booking Liability / etc.)
    // =========================================================================

    /**
     * Posted debit/credit totals at the account_group level (full hierarchy:
     * type → category → group).  Returns all account types.
     */
    @Query(value = """
            SELECT at.name AS accountType,
                   ac.name AS accountCategory,
                   ag.name AS accountGroup,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY at.name, ac.name, ag.name
            ORDER BY at.name, ac.name, ag.name
            """, nativeQuery = true)
    List<AccountGroupSummaryProjection> summarizeByAccountGroup(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Posted debit/credit totals at the account_group level filtered to a single
     * account_type (e.g. "LIABILITY" for payable drill-down, "ASSET" for receivable
     * drill-down).  Uses exact match on the account_type.name column.
     */
    @Query(value = """
            SELECT at.name AS accountType,
                   ac.name AS accountCategory,
                   ag.name AS accountGroup,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
              AND at.name = :accountTypeName
            GROUP BY at.name, ac.name, ag.name
            ORDER BY ac.name, ag.name
            """, nativeQuery = true)
    List<AccountGroupSummaryProjection> summarizeByAccountGroupForType(
            @Param("organizationId") long organizationId,
            @Param("accountTypeName") String accountTypeName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // LEVEL 4 — Chart of Account (individual COA codes)
    // =========================================================================

    /** Posted debit/credit totals at chart-of-account level for the org + date range. */
    @Query(value = """
            SELECT at.name  AS accountType,
                   ac.name  AS accountCategory,
                   coa.code AS accountCode,
                   coa.name AS accountName,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY at.name, ac.name, coa.code, coa.name
            ORDER BY at.name, ac.name, coa.code
            """, nativeQuery = true)
    List<AccountSummaryProjection> summarizeByAccount(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Posted debit/credit totals for a single account code, scoped to org + date range. */
    @Query(value = """
            SELECT at.name  AS accountType,
                   ac.name  AS accountCategory,
                   coa.code AS accountCode,
                   coa.name AS accountName,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND coa.code = :accountCode
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY at.name, ac.name, coa.code, coa.name
            """, nativeQuery = true)
    List<AccountSummaryProjection> summarizeByAccountCode(
            @Param("organizationId") long organizationId,
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // TREND / MONTHLY
    // =========================================================================

    /** Monthly posted debit/credit for a single account code (accounting-derived trends). */
    @Query(value = """
            SELECT YEAR(je.created_date)  AS yr,
                   MONTH(je.created_date) AS monthNo,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je     ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa ON coa.id = jd.chart_of_account_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND coa.code = :accountCode
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(je.created_date), MONTH(je.created_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyAmountProjection> monthlyByAccountCode(
            @Param("organizationId") long organizationId,
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Monthly posted debit/credit totals for an entire account_type (e.g. "LIABILITY").
     * Used for payable / receivable monthly trend charts that should reflect all accounts
     * under that type rather than a single COA code.
     */
    @Query(value = """
            SELECT YEAR(je.created_date)  AS yr,
                   MONTH(je.created_date) AS monthNo,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND at.name = :accountTypeName
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(je.created_date), MONTH(je.created_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyAmountProjection> monthlyByAccountType(
            @Param("organizationId") long organizationId,
            @Param("accountTypeName") String accountTypeName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Monthly posted debit/credit totals for a single account_category
     * (e.g. "CURRENT LIABILITY").  Used for per-category trend charts.
     */
    @Query(value = """
            SELECT YEAR(je.created_date)  AS yr,
                   MONTH(je.created_date) AS monthNo,
                   COALESCE(SUM(jd.debit_amount), 0)  AS totalDebit,
                   COALESCE(SUM(jd.credit_amount), 0) AS totalCredit
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND ac.name = :accountCategoryName
              AND je.created_date BETWEEN :startDate AND :endDate
            GROUP BY YEAR(je.created_date), MONTH(je.created_date)
            ORDER BY yr, monthNo
            """, nativeQuery = true)
    List<MonthlyAmountProjection> monthlyByAccountCategory(
            @Param("organizationId") long organizationId,
            @Param("accountCategoryName") String accountCategoryName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
