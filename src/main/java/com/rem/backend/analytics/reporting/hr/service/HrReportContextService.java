package com.rem.backend.analytics.reporting.hr.service;

import com.rem.backend.usermanagement.entity.Role;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.usermanagement.service.RoleService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the reporting tenant context (organization) for the HR reporting module and
 * enforces that the caller holds an HR-authorized role.
 * <p>
 * As with the admin and accountant reporting contexts, the organization is always derived
 * from the authenticated user (never trusted from the client). In addition, this service
 * asserts the user carries one of {@link #ALLOWED_ROLES} so HR data (compensation, leave,
 * attendance) stays restricted even if the endpoint prefix permission is broadened.
 */
@Service
@AllArgsConstructor
public class HrReportContextService {

    /** Roles permitted to view the HR reporting module. */
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "HR_ROLE", "HR_MANAGER_ROLE", "ADMIN_ROLE", "FULL_ADMIN_ROLE");

    private final UserRepo userRepo;
    private final RoleService roleService;

    /**
     * Resolves the organization id for the authenticated user after verifying the user holds
     * an HR-authorized role.
     *
     * @param username the authenticated username (from the JWT, via request attribute)
     * @return the organization id the user belongs to
     * @throws IllegalArgumentException if the user cannot be resolved or lacks an allowed role
     */
    public long resolveOrganizationId(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Unauthenticated request: missing user context");
        }
        User user = userRepo.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new IllegalArgumentException("Active user not found: " + username));

        if (!hasAllowedRole(user.getId())) {
            throw new IllegalArgumentException(
                    "User is not authorized to access HR reporting: " + username);
        }
        return user.getOrganizationId();
    }

    private boolean hasAllowedRole(long userId) {
        Set<UserRoles> userRoles = roleService.getUserRoles(userId);
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        Set<String> roleNames = new HashSet<>();
        for (UserRoles ur : userRoles) {
            Optional<Role> role = roleService.getRoleById(ur.getRoleId());
            role.ifPresent(r -> roleNames.add(r.getName() == null ? "" : r.getName().toUpperCase()));
        }
        return roleNames.stream().anyMatch(ALLOWED_ROLES::contains);
    }
}
