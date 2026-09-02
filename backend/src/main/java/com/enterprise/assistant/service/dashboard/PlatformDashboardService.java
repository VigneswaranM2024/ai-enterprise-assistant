package com.enterprise.assistant.service.dashboard;

import com.enterprise.assistant.dto.response.PlatformDashboardResponse;

/**
 * Service Contract for Platform-Wide Administration Dashboard Analytics.
 */
public interface PlatformDashboardService {

    /**
     * Computes global platform-wide metrics across all tenants.
     *
     * @return PlatformDashboardResponse
     */
    PlatformDashboardResponse getPlatformDashboardMetrics();
}
