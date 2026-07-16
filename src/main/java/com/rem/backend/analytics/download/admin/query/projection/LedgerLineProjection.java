package com.rem.backend.analytics.download.admin.query.projection;

import java.time.LocalDateTime;

/**
 * A single posted journal line for general ledger / account statement exports.
 */
public interface LedgerLineProjection {
    Long getJournalEntryId();

    LocalDateTime getTransactionDate();

    String getReferenceType();

    String getJournalDescription();

    String getLineDescription();

    String getAccountType();

    String getAccountCategory();

    String getAccountCode();

    String getAccountName();

    Double getDebitAmount();

    Double getCreditAmount();
}
