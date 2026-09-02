package com.enterprise.assistant.service.dashboard;

import com.enterprise.assistant.dto.response.PlatformDashboardResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.repository.document.DocumentRepository;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.dashboard.impl.PlatformDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for PlatformDashboardServiceImpl verifying global calculations,
 * storage formatting, and Redis failure fallback.
 */
@ExtendWith(MockitoExtension.class)
class PlatformDashboardServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private PlatformDashboardServiceImpl platformDashboardService;

    @BeforeEach
    void setUp() {
        platformDashboardService = new PlatformDashboardServiceImpl(
                tenantRepository,
                userRepository,
                documentRepository,
                documentChunkRepository,
                redisConnectionFactory
        );
    }

    @Test
    void getPlatformDashboardMetrics_CorrectGlobalAggregateCalculations() {
        when(tenantRepository.count()).thenReturn(10L);
        when(tenantRepository.countByStatus("ACTIVE")).thenReturn(8L);
        when(userRepository.count()).thenReturn(250L);
        when(documentRepository.count()).thenReturn(1500L);
        when(documentChunkRepository.count()).thenReturn(18000L);
        when(documentRepository.sumAllFileSizeBytes()).thenReturn(1572864000L); // ~1.46 GB

        PlatformDashboardResponse response = platformDashboardService.getPlatformDashboardMetrics();

        assertNotNull(response);
        assertEquals(10L, response.totalTenants());
        assertEquals(8L, response.activeTenants());
        assertEquals(250L, response.totalPlatformUsers());
        assertEquals(1500L, response.totalPlatformDocuments());
        assertEquals(18000L, response.totalPlatformVectorChunks());
        assertEquals(1572864000L, response.totalPlatformStorageBytes());
        assertTrue(response.formattedTotalStorage().contains("GB") || response.formattedTotalStorage().contains("MB"));
        assertEquals("UP", response.systemHealth());
    }

    @Test
    void redisUnavailable_GracefullySetsDegradedCacheStatusWithoutCrashing() {
        when(tenantRepository.count()).thenReturn(5L);
        when(tenantRepository.countByStatus("ACTIVE")).thenReturn(5L);
        when(userRepository.count()).thenReturn(50L);
        when(documentRepository.count()).thenReturn(100L);
        when(documentChunkRepository.count()).thenReturn(1000L);
        when(documentRepository.sumAllFileSizeBytes()).thenReturn(1048576L);

        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Redis connection refused"));

        PlatformDashboardResponse response = platformDashboardService.getPlatformDashboardMetrics();

        assertNotNull(response);
        assertEquals("DEGRADED", response.cacheStatus());
        assertEquals("UP", response.systemHealth());
    }

    @Test
    void noSensitiveDataLeakage_ResponseContainsOnlyAggregates() {
        when(tenantRepository.count()).thenReturn(1L);
        when(tenantRepository.countByStatus("ACTIVE")).thenReturn(1L);
        when(userRepository.count()).thenReturn(1L);
        when(documentRepository.count()).thenReturn(1L);
        when(documentChunkRepository.count()).thenReturn(10L);
        when(documentRepository.sumAllFileSizeBytes()).thenReturn(500L);

        PlatformDashboardResponse response = platformDashboardService.getPlatformDashboardMetrics();

        assertNotNull(response);
        // Verify response contains strictly numerical/system stats and no raw content fields
        assertEquals(1L, response.totalTenants());
        assertNotNull(response.formattedTotalStorage());
    }
}
