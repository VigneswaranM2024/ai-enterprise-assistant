package com.enterprise.assistant.service.dashboard.impl;

import com.enterprise.assistant.domain.chat.ChatSessionStatus;
import com.enterprise.assistant.domain.document.DocumentCategory;
import com.enterprise.assistant.domain.document.DocumentStatus;
import com.enterprise.assistant.dto.response.TenantDashboardResponse;
import com.enterprise.assistant.repository.audit.AuditLogRepository;
import com.enterprise.assistant.repository.chat.ChatMessageRepository;
import com.enterprise.assistant.repository.chat.ChatSessionRepository;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.repository.document.DocumentRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.dashboard.TenantDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Implementation of TenantDashboardService providing aggregate metrics
 * for users, documents, storage, chat, and audit event logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDashboardServiceImpl implements TenantDashboardService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-summary", key = "'tenant:' + #principal.tenantId + ':dashboard:summary'", unless = "#result == null", sync = true)
    public TenantDashboardResponse getTenantDashboardMetrics(UserPrincipal principal) {
        validatePrincipal(principal);
        UUID tenantId = principal.getTenantId();

        log.info("Computing tenant dashboard metrics for tenant: {}", tenantId);

        // 1. User Metrics
        long totalUsers = userRepository.countByTenantId(tenantId);
        long activeUsers = userRepository.countByTenantIdAndIsActive(tenantId, true);
        long inactiveUsers = userRepository.countByTenantIdAndIsActive(tenantId, false);

        // 2. Document & Storage Metrics
        long totalDocuments = documentRepository.countByTenantId(tenantId);
        long readyDocuments = documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.READY);
        long processingDocuments = documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.PROCESSING);
        long failedDocuments = documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.FAILED);
        long totalStorageBytes = documentRepository.sumFileSizeBytesByTenantId(tenantId);
        long totalDocumentChunks = documentChunkRepository.countByTenantId(tenantId);

        // Document Category Breakdown
        Map<String, Long> categoryMap = new LinkedHashMap<>();
        for (DocumentCategory category : DocumentCategory.values()) {
            categoryMap.put(category.name(), 0L);
        }
        List<Object[]> rawCategoryCounts = documentRepository.countByCategoryByTenantId(tenantId);
        if (rawCategoryCounts != null) {
            for (Object[] row : rawCategoryCounts) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    String catName = row[0].toString();
                    Long count = ((Number) row[1]).longValue();
                    categoryMap.put(catName, count);
                }
            }
        }

        // 3. Chat Metrics
        long activeChatSessions = chatSessionRepository.countByTenantIdAndStatus(tenantId, ChatSessionStatus.ACTIVE);
        long totalChatMessages = chatMessageRepository.countByTenantId(tenantId);

        // 4. Audit Metrics (Last 24 Hours)
        OffsetDateTime since24h = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
        long auditEventsPast24h = auditLogRepository.countByTenantIdAndCreatedAtAfter(tenantId, since24h);
        long failedAuditOperationsPast24h = auditLogRepository.countByTenantIdAndStatusAndCreatedAtAfter(tenantId, "FAILURE", since24h);

        return new TenantDashboardResponse(
                tenantId,
                totalUsers,
                activeUsers,
                inactiveUsers,
                totalDocuments,
                readyDocuments,
                processingDocuments,
                failedDocuments,
                categoryMap,
                totalStorageBytes,
                totalDocumentChunks,
                activeChatSessions,
                totalChatMessages,
                auditEventsPast24h,
                failedAuditOperationsPast24h
        );
    }

    private void validatePrincipal(UserPrincipal principal) {
        if (principal == null || principal.getTenantId() == null) {
            throw new IllegalArgumentException("Authenticated UserPrincipal with non-null tenantId is required");
        }
    }
}
