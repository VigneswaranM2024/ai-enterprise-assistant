package com.enterprise.assistant.controller.dashboard;

import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.PlatformDashboardResponse;
import com.enterprise.assistant.service.dashboard.PlatformDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing global platform administration aggregate analytics.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Platform Dashboard", description = "Endpoints for retrieving global platform-wide administration metrics")
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    @GetMapping("/platform")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get platform-wide admin metrics", description = "Platform Admin endpoint to retrieve global system metrics across all tenants")
    public ResponseEntity<ApiResponse<PlatformDashboardResponse>> getPlatformDashboardMetrics() {
        PlatformDashboardResponse response = platformDashboardService.getPlatformDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Platform metrics retrieved successfully", response));
    }
}
