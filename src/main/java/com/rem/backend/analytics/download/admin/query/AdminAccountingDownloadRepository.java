package com.rem.backend.analytics.download.admin.query;

import com.rem.backend.accountingmanagement.entity.JournalDetailEntry;
import com.rem.backend.analytics.download.admin.query.projection.LedgerLineProjection;
import com.rem.backend.analytics.reporting.admin.query.projection.AccountSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only repository for downloadable accounting statements.
 * Queries are isolated from transactional accounting code.
 */
@Repository
public interface AdminAccountingDownloadRepository extends JpaRepository<JournalDetailEntry, Long> {

    /** Cumulative posted debit/credit per account up to and including {@code asOfDate}. */
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
              AND je.created_date <= :asOfDate
            GROUP BY at.name, ac.name, coa.code, coa.name
            ORDER BY at.name, ac.name, coa.code
            """, nativeQuery = true)
    List<AccountSummaryProjection> summarizeByAccountAsOf(
            @Param("organizationId") long organizationId,
            @Param("asOfDate") LocalDateTime asOfDate);

    /** Posted debit/credit per account strictly before {@code beforeDate} (opening balances). */
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
              AND je.created_date < :beforeDate
            GROUP BY at.name, ac.name, coa.code, coa.name
            ORDER BY at.name, ac.name, coa.code
            """, nativeQuery = true)
    List<AccountSummaryProjection> summarizeByAccountBefore(
            @Param("organizationId") long organizationId,
            @Param("beforeDate") LocalDateTime beforeDate);

    /** Posted debit/credit per account within the period (movement). */
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
    List<AccountSummaryProjection> summarizeByAccountPeriod(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Posted journal lines for general ledger / account statement. */
    @Query(value = """
            SELECT je.id              AS journalEntryId,
                   je.created_date    AS transactionDate,
                   je.reference_type  AS referenceType,
                   je.description     AS journalDescription,
                   jd.description     AS lineDescription,
                   at.name            AS accountType,
                   ac.name            AS accountCategory,
                   coa.code           AS accountCode,
                   coa.name           AS accountName,
                   jd.debit_amount    AS debitAmount,
                   jd.credit_amount   AS creditAmount
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
              AND (:accountCode IS NULL OR coa.code = :accountCode)
            ORDER BY coa.code, je.created_date, je.id, jd.id
            """, nativeQuery = true)
    List<LedgerLineProjection> ledgerLines(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("accountCode") String accountCode);

    /** All posted journal lines ordered by voucher date (journal register). */
    @Query(value = """
            SELECT je.id              AS journalEntryId,
                   je.created_date    AS transactionDate,
                   je.reference_type  AS referenceType,
                   je.description     AS journalDescription,
                   jd.description     AS lineDescription,
                   at.name            AS accountType,
                   ac.name            AS accountCategory,
                   coa.code           AS accountCode,
                   coa.name           AS accountName,
                   jd.debit_amount    AS debitAmount,
                   jd.credit_amount   AS creditAmount
            FROM journal_detail_entry jd
            JOIN journal_entry je       ON je.id = jd.journal_entry_id
            JOIN chart_of_account coa   ON coa.id = jd.chart_of_account_id
            JOIN account_group ag       ON ag.id = coa.account_group_id
            JOIN account_category ac    ON ac.id = ag.account_category_id
            JOIN account_type at        ON at.id = ac.account_type_id
            WHERE je.organization_id = :organizationId
              AND je.status = 'POSTED'
              AND je.created_date BETWEEN :startDate AND :endDate
            ORDER BY je.created_date, je.id, jd.id
            """, nativeQuery = true)
    List<LedgerLineProjection> journalRegisterLines(
            @Param("organizationId") long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
