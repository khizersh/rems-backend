package com.rem.backend.analytics.reporting.admin.service;

import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.repository.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves the reporting tenant context (organization) from the authenticated principal.
 * <p>
 * Admin reporting never trusts a client-supplied organizationId; it always derives the org
 * from the logged-in user so reports are automatically tenant-scoped.
 */
@Service
@AllArgsConstructor
public class AdminReportContextService {

    private final UserRepo userRepo;

    /**
     * @param username the authenticated username (from the JWT, via request attribute)
     * @return the organization id the user belongs to
     * @throws IllegalArgumentException if the user cannot be resolved
     */
    public long resolveOrganizationId(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Unauthenticated request: missing user context");
        }
        User user = userRepo.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new IllegalArgumentException("Active user not found: " + username));
        return user.getOrganizationId();
    }
}
