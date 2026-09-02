package com.enterprise.assistant.dto.response;

import java.util.Map;
import java.util.UUID;

/**
 * DTO carrying tenant-scoped aggregate dashboard metrics and analytics.
 */
public record TenantDashboardResponse(
        UUID tenantId,
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long totalDocuments,
        long readyDocuments,
        long processingDocuments,
        long failedDocuments,
        Map<String, Long> documentCategoryBreakdown,
        long totalStorageBytes,
        long totalDocumentChunks,
        long activeChatSessions,
        long totalChatMessages,
        long auditEventsPast24h,
        long failedAuditOperationsPast24h
) {}
