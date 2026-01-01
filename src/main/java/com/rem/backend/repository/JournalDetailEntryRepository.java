package com.rem.backend.repository;

import com.rem.backend.entity.account.JournalDetailEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalDetailEntryRepository extends JpaRepository<JournalDetailEntry, Long> {
    List<JournalDetailEntry> findAllByJournalEntryId(long journalEntryId);
    List<JournalDetailEntry> findAllByChartOfAccountId(long chartOfAccountId);
    void deleteAllByJournalEntryId(long journalEntryId);
}

