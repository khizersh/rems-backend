package com.rem.backend.analytics.reporting.admin.query.projection;

/**
 * Aggregated posted debit/credit totals at the account_group level, carrying the
 * full parent hierarchy (type → category → group) for drill-down reporting.
 * <p>
 * This is the "4th level" of the COA hierarchy used by payable / receivable deep-dives:
 * account_type → account_category → account_group → chart_of_account → journal_detail_entry
 */
public interface AccountGroupSummaryProjection {

    String getAccountType();

    String getAccountCategory();

    String getAccountGroup();

    Double getTotalDebit();

    Double getTotalCredit();
}
