package com.enterprise.assistant.controller.dashboard;

import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.TenantDashboardResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.dashboard.TenantDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing tenant-scoped analytics and aggregate dashboard metrics.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tenant Dashboard", description = "Endpoints for retrieving tenant-scoped aggregate metrics and analytics")
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    @GetMapping("/tenant")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get tenant dashboard metrics", description = "Tenant Admin endpoint to retrieve aggregate metrics for users, documents, storage, chat, and audit events")
    public ResponseEntity<ApiResponse<TenantDashboardResponse>> getTenantDashboardMetrics(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TenantDashboardResponse response = tenantDashboardService.getTenantDashboardMetrics(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Tenant dashboard metrics retrieved successfully", response));
    }
}
