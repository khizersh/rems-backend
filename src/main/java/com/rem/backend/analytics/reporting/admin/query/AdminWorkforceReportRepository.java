package com.rem.backend.analytics.reporting.admin.query;

import com.rem.backend.usermanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Reporting counts over system users. {@code UserRepo} has no count method, so the
 * reporting module adds its own org-scoped count here (non-breaking, isolated).
 */
@Repository
public interface AdminWorkforceReportRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT COUNT(*) FROM users WHERE organization_id = :organizationId AND is_active = 1",
            nativeQuery = true)
    long countActiveUsers(@Param("organizationId") long organizationId);
}
