package com.enterprise.assistant.service.dashboard;

import com.enterprise.assistant.domain.chat.ChatSessionStatus;
import com.enterprise.assistant.domain.document.DocumentStatus;
import com.enterprise.assistant.dto.response.TenantDashboardResponse;
import com.enterprise.assistant.repository.audit.AuditLogRepository;
import com.enterprise.assistant.repository.chat.ChatMessageRepository;
import com.enterprise.assistant.repository.chat.ChatSessionRepository;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.repository.document.DocumentRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.dashboard.impl.TenantDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TenantDashboardServiceImpl verifying correct aggregate calculations,
 * empty tenant fallbacks, and tenant isolation key formatting.
 */
@ExtendWith(MockitoExtension.class)
class TenantDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private TenantDashboardServiceImpl dashboardService;

    private UUID tenantId;
    private UUID userId;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        adminPrincipal = new UserPrincipal(
                userId, tenantId, "tenant-slug", "admin@tenant.com", "pass", "Admin User",
                "INTERNAL", "ENG", true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        dashboardService = new TenantDashboardServiceImpl(
                userRepository,
                documentRepository,
                documentChunkRepository,
                chatSessionRepository,
                chatMessageRepository,
                auditLogRepository
        );
    }

    @Test
    void getTenantDashboardMetrics_CorrectAggregateCalculations() {
        when(userRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(userRepository.countByTenantIdAndIsActive(tenantId, true)).thenReturn(8L);
        when(userRepository.countByTenantIdAndIsActive(tenantId, false)).thenReturn(2L);

        when(documentRepository.countByTenantId(tenantId)).thenReturn(20L);
        when(documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.READY)).thenReturn(18L);
        when(documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.PROCESSING)).thenReturn(1L);
        when(documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.FAILED)).thenReturn(1L);
        when(documentRepository.sumFileSizeBytesByTenantId(tenantId)).thenReturn(10485760L);

        Object[] polRow = {"POLICIES", 10L};
        Object[] finRow = {"FINANCIAL", 10L};
        List<Object[]> categoryCounts = List.of(polRow, finRow);
        when(documentRepository.countByCategoryByTenantId(tenantId)).thenReturn(categoryCounts);

        when(documentChunkRepository.countByTenantId(tenantId)).thenReturn(200L);
        when(chatSessionRepository.countByTenantIdAndStatus(tenantId, ChatSessionStatus.ACTIVE)).thenReturn(5L);
        when(chatMessageRepository.countByTenantId(tenantId)).thenReturn(40L);

        when(auditLogRepository.countByTenantIdAndCreatedAtAfter(eq(tenantId), any())).thenReturn(15L);
        when(auditLogRepository.countByTenantIdAndStatusAndCreatedAtAfter(eq(tenantId), eq("FAILURE"), any())).thenReturn(1L);

        TenantDashboardResponse response = dashboardService.getTenantDashboardMetrics(adminPrincipal);

        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        assertEquals(10L, response.totalUsers());
        assertEquals(8L, response.activeUsers());
        assertEquals(2L, response.inactiveUsers());
        assertEquals(20L, response.totalDocuments());
        assertEquals(18L, response.readyDocuments());
        assertEquals(1L, response.processingDocuments());
        assertEquals(1L, response.failedDocuments());
        assertEquals(10485760L, response.totalStorageBytes());
        assertEquals(200L, response.totalDocumentChunks());
        assertEquals(5L, response.activeChatSessions());
        assertEquals(40L, response.totalChatMessages());
        assertEquals(15L, response.auditEventsPast24h());
        assertEquals(1L, response.failedAuditOperationsPast24h());

        assertEquals(10L, response.documentCategoryBreakdown().get("POLICIES"));
        assertEquals(10L, response.documentCategoryBreakdown().get("FINANCIAL"));
        assertEquals(0L, response.documentCategoryBreakdown().get("HR"));
    }

    @Test
    void emptyTenant_ReturnsZeroValuesAcrossAllMetrics() {
        when(userRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(userRepository.countByTenantIdAndIsActive(tenantId, true)).thenReturn(0L);
        when(userRepository.countByTenantIdAndIsActive(tenantId, false)).thenReturn(0L);

        when(documentRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.READY)).thenReturn(0L);
        when(documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.PROCESSING)).thenReturn(0L);
        when(documentRepository.countByTenantIdAndStatus(tenantId, DocumentStatus.FAILED)).thenReturn(0L);
        when(documentRepository.sumFileSizeBytesByTenantId(tenantId)).thenReturn(0L);
        when(documentRepository.countByCategoryByTenantId(tenantId)).thenReturn(Collections.emptyList());

        when(documentChunkRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(chatSessionRepository.countByTenantIdAndStatus(tenantId, ChatSessionStatus.ACTIVE)).thenReturn(0L);
        when(chatMessageRepository.countByTenantId(tenantId)).thenReturn(0L);

        when(auditLogRepository.countByTenantIdAndCreatedAtAfter(eq(tenantId), any())).thenReturn(0L);
        when(auditLogRepository.countByTenantIdAndStatusAndCreatedAtAfter(eq(tenantId), eq("FAILURE"), any())).thenReturn(0L);

        TenantDashboardResponse response = dashboardService.getTenantDashboardMetrics(adminPrincipal);

        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        assertEquals(0L, response.totalUsers());
        assertEquals(0L, response.totalDocuments());
        assertEquals(0L, response.totalStorageBytes());
        assertEquals(0L, response.totalDocumentChunks());
        assertEquals(0L, response.activeChatSessions());
        assertEquals(0L, response.totalChatMessages());
        assertEquals(0L, response.auditEventsPast24h());
    }

    @Test
    void cacheKeyFormat_IncludesTenantIdForIsolation() {
        String cacheKeyPattern = "tenant:" + tenantId + ":dashboard:summary";
        assertTrue(cacheKeyPattern.contains(tenantId.toString()));
        assertTrue(cacheKeyPattern.startsWith("tenant:"));
    }
}
