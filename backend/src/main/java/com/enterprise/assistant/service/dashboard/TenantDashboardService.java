package com.enterprise.assistant.service.dashboard;

import com.enterprise.assistant.dto.response.TenantDashboardResponse;
import com.enterprise.assistant.security.user.UserPrincipal;

/**
 * Service Contract for Tenant Dashboard Analytics and Metrics.
 */
public interface TenantDashboardService {

    /**
     * Retrieves tenant-scoped aggregate metrics for users, documents, storage, chat, and audit logs.
     *
     * @param principal authenticated UserPrincipal (tenantId sourced strictly from principal)
     * @return TenantDashboardResponse
     */
    TenantDashboardResponse getTenantDashboardMetrics(UserPrincipal principal);
}
