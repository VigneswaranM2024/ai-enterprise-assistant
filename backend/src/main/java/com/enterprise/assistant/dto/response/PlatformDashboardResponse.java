package com.enterprise.assistant.dto.response;

/**
 * DTO carrying platform-wide aggregate metrics for super/platform administrators.
 */
public record PlatformDashboardResponse(
        long totalTenants,
        long activeTenants,
        long totalPlatformUsers,
        long totalPlatformDocuments,
        long totalPlatformVectorChunks,
        long totalPlatformStorageBytes,
        String formattedTotalStorage,
        String systemHealth,
        String cacheStatus
) {}
