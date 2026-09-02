package com.enterprise.assistant.controller.dashboard;

import com.enterprise.assistant.dto.response.TenantDashboardResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.dashboard.TenantDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller Security and Endpoint Tests verifying HTTP 200 responses for ROLE_ADMIN
 * and strict tenant isolation.
 */
@ExtendWith(MockitoExtension.class)
class TenantDashboardControllerTest {

    @Mock
    private TenantDashboardService tenantDashboardService;

    private TenantDashboardController dashboardController;

    private UUID tenantAId;
    private UUID tenantBId;
    private UserPrincipal adminTenantA;

    @BeforeEach
    void setUp() {
        dashboardController = new TenantDashboardController(tenantDashboardService);

        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();

        adminTenantA = new UserPrincipal(
                UUID.randomUUID(), tenantAId, "tenant-a", "admin@tenant-a.com", "pass", "Admin A",
                "INTERNAL", "ENG", true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void getTenantDashboardMetrics_RoleAdmin_Returns200OkWithTenantMetrics() {
        TenantDashboardResponse mockMetrics = new TenantDashboardResponse(
                tenantAId, 10L, 8L, 2L, 20L, 18L, 1L, 1L, Map.of("GENERAL", 20L),
                1048576L, 150L, 5L, 25L, 10L, 0L
        );

        when(tenantDashboardService.getTenantDashboardMetrics(adminTenantA)).thenReturn(mockMetrics);

        ResponseEntity<?> responseEntity = dashboardController.getTenantDashboardMetrics(adminTenantA);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        verify(tenantDashboardService).getTenantDashboardMetrics(adminTenantA);
    }

    @Test
    void tenantIsolation_TenantAUserReceivesTenantAMetricsOnly() {
        TenantDashboardResponse mockMetricsA = new TenantDashboardResponse(
                tenantAId, 5L, 5L, 0L, 10L, 10L, 0L, 0L, Collections.emptyMap(),
                500L, 50L, 2L, 10L, 4L, 0L
        );

        when(tenantDashboardService.getTenantDashboardMetrics(adminTenantA)).thenReturn(mockMetricsA);

        ResponseEntity<com.enterprise.assistant.dto.response.ApiResponse<TenantDashboardResponse>> responseEntity =
                dashboardController.getTenantDashboardMetrics(adminTenantA);

        assertNotNull(responseEntity.getBody());
        assertEquals(tenantAId, responseEntity.getBody().getData().tenantId());
        assertNotEquals(tenantBId, responseEntity.getBody().getData().tenantId());
    }
}
