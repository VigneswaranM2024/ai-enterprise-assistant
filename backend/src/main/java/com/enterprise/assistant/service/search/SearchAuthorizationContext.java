package com.enterprise.assistant.service.search;

import com.enterprise.assistant.domain.user.SecurityClassification;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Immutable authorization context used for database-level access filtering during semantic search.
 * <p>
 * All values are sourced exclusively from the authenticated JWT principal — never from the client request body.
 * </p>
 *
 * <p>Access control rules enforced by the native SQL query:</p>
 * <ul>
 *   <li>Tenant isolation: chunk.tenant_id must match user's tenantId</li>
 *   <li>Security classification: chunk.security_classification ordinal &le; user's clearance ordinal</li>
 *   <li>Allowed roles: chunk.allowed_roles is empty/null OR user has at least one matching role</li>
 *   <li>Allowed departments: chunk.allowed_departments is empty/null OR user's departmentCode is in the list</li>
 * </ul>
 */
@Getter
@Builder
public class SearchAuthorizationContext {

    /** Tenant ID from JWT principal. Enforces hard multi-tenant isolation. */
    private final UUID tenantId;

    /** User's security clearance level name (e.g. "INTERNAL", "CONFIDENTIAL"). */
    private final String userClearance;

    /** All role names the user holds (e.g. ["ROLE_ADMIN", "ROLE_EMPLOYEE"]). Never null. */
    private final List<String> userRoles;

    /**
     * The department code from the user's assigned Department entity (e.g. "HR").
     * Null when user has no department assignment.
     */
    private final String userDepartmentCode;

    /**
     * Sourced exclusively from the authenticated UserPrincipal.
     */
    public static SearchAuthorizationContext fromUserPrincipal(com.enterprise.assistant.security.user.UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("UserPrincipal must not be null");
        }

        java.util.List<String> roles = principal.getAuthorities() != null
                ? principal.getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList();

        return SearchAuthorizationContext.builder()
                .tenantId(principal.getTenantId())
                .userClearance(principal.getSecurityClassification())
                .userRoles(roles)
                .userDepartmentCode(principal.getDepartmentCode())
                .build();
    }

    /**
     * Applies the same ABAC rules used by
     * {@code DocumentChunkRepository#findSimilarChunksAuthorized} to a single document, for paths
     * that cannot push the predicate into a vector query (for example listing documents in chat).
     *
     * <ul>
     *   <li>clearance: document classification ordinal &le; caller clearance ordinal</li>
     *   <li>roles: allowed roles null/empty, or at least one match with the caller's roles</li>
     *   <li>departments: allowed departments null/empty, or the caller's department is listed</li>
     * </ul>
     *
     * @param documentClassification document security classification (null is treated as INTERNAL,
     *                               matching the entity default)
     * @param allowedRoles           document role restrictions; may be null
     * @param allowedDepartments     document department restrictions; may be null
     * @return true when the caller may see the document
     */
    public boolean canAccess(
            SecurityClassification documentClassification,
            String[] allowedRoles,
            String[] allowedDepartments) {

        SecurityClassification required =
                documentClassification != null ? documentClassification : SecurityClassification.INTERNAL;

        if (required.ordinal() > parseClearance(userClearance).ordinal()) {
            return false;
        }
        if (!isRoleAllowed(allowedRoles)) {
            return false;
        }
        return isDepartmentAllowed(allowedDepartments);
    }

    /** Parses the caller clearance, failing closed to PUBLIC when absent or unrecognised. */
    private static SecurityClassification parseClearance(String clearance) {
        if (clearance == null || clearance.isBlank()) {
            return SecurityClassification.PUBLIC;
        }
        try {
            return SecurityClassification.valueOf(clearance.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SecurityClassification.PUBLIC;
        }
    }

    private boolean isRoleAllowed(String[] allowedRoles) {
        if (allowedRoles == null || allowedRoles.length == 0) {
            return true;
        }
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        for (String role : allowedRoles) {
            if (role != null && userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDepartmentAllowed(String[] allowedDepartments) {
        if (allowedDepartments == null || allowedDepartments.length == 0) {
            return true;
        }
        if (userDepartmentCode == null || userDepartmentCode.isBlank()) {
            return false;
        }
        for (String department : allowedDepartments) {
            if (userDepartmentCode.equals(department)) {
                return true;
            }
        }
        return false;
    }
}
