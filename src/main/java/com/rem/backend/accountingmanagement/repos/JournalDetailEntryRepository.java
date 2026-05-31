package com.rem.backend.accountingmanagement.repos;

import com.rem.backend.accountingmanagement.entity.JournalDetailEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalDetailEntryRepository extends JpaRepository<JournalDetailEntry, Long> {
    List<JournalDetailEntry> findAllByJournalEntryId(long journalEntryId);
    List<JournalDetailEntry> findAllByChartOfAccountId(long chartOfAccountId);
    void deleteAllByJournalEntryId(long journalEntryId);
}


