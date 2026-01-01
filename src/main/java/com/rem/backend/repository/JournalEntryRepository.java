package com.rem.backend.repository;

import com.rem.backend.entity.account.JournalEntry;
import com.rem.backend.enums.JournalEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findAllByOrganizationId(long organizationId);
    List<JournalEntry> findAllByOrganizationIdAndStatus(long organizationId, JournalEntryStatus status);
    List<JournalEntry> findAllByOrganizationIdAndCreatedDateBetween(long organizationId, LocalDateTime startDate, LocalDateTime endDate);
    List<JournalEntry> findAllByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}

