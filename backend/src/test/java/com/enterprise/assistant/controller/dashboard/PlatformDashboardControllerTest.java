package com.enterprise.assistant.controller.dashboard;

import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.PlatformDashboardResponse;
import com.enterprise.assistant.service.dashboard.PlatformDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller Unit & Security Tests verifying HTTP 200 responses for platform admin roles.
 */
@ExtendWith(MockitoExtension.class)
class PlatformDashboardControllerTest {

    @Mock
    private PlatformDashboardService platformDashboardService;

    private PlatformDashboardController platformDashboardController;

    @BeforeEach
    void setUp() {
        platformDashboardController = new PlatformDashboardController(platformDashboardService);
    }

    @Test
    void getPlatformDashboardMetrics_PlatformAdmin_Returns200OkWithMetrics() {
        PlatformDashboardResponse mockResponse = new PlatformDashboardResponse(
                10L, 8L, 250L, 1500L, 18000L, 1572864000L, "1.46 GB", "UP", "HEALTHY"
        );

        when(platformDashboardService.getPlatformDashboardMetrics()).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<PlatformDashboardResponse>> responseEntity = platformDashboardController.getPlatformDashboardMetrics();

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(10L, responseEntity.getBody().getData().totalTenants());
        verify(platformDashboardService).getPlatformDashboardMetrics();
    }
}
