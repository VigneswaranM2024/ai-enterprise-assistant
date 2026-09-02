package com.enterprise.assistant.controller.role;

import com.enterprise.assistant.dto.request.CreateRoleRequest;
import com.enterprise.assistant.dto.request.UpdateRoleRequest;
import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.RoleResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.role.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Role Governance REST Controller exposing RBAC role CRUD endpoints.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Governance", description = "Endpoints for managing tenant RBAC roles and permissions")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get tenant roles", description = "Lists all system and custom RBAC roles available in the current tenant")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<RoleResponse> response = roleService.getTenantRoles(currentUser.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get role by ID", description = "Inspect role metadata and assigned permission codes")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        RoleResponse response = roleService.getRoleById(currentUser.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create custom tenant role", description = "Admin endpoint to create a custom enterprise role within the tenant")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateRoleRequest request
    ) {
        RoleResponse response = roleService.createRole(currentUser.getTenantId(), request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Role created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update role details & permissions", description = "Admin endpoint to update role description and permission codes")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        RoleResponse response = roleService.updateRole(currentUser.getTenantId(), id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete custom role", description = "Admin endpoint to delete a custom role (system roles cannot be deleted)")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        roleService.deleteRole(currentUser.getTenantId(), id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }
}
