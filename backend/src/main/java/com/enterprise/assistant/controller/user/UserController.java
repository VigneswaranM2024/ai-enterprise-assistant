package com.enterprise.assistant.controller.user;

import com.enterprise.assistant.dto.request.CreateUserRequest;
import com.enterprise.assistant.dto.request.UpdateUserRequest;
import com.enterprise.assistant.dto.request.UpdateUserStatusRequest;
import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.PageResponse;
import com.enterprise.assistant.dto.response.UserResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User Management REST Controller exposing CRUD, search, status, and role assignment endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for searching, creating, updating, activating, and deactivating enterprise users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search tenant users", description = "Admin endpoint to search users with pagination, sorting, and name/email filters")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getTenantUsers(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Fuzzy search query for full name or email") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size limit") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort attribute") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<UserResponse> response = userService.getTenantUsers(currentUser.getTenantId(), query, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get current authenticated user profile", description = "Returns full profile details and permissions for the logged-in user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserResponse response = userService.getCurrentUserProfile(currentUser.getTenantId(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user details by ID", description = "Admin endpoint to inspect user profile by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        UserResponse response = userService.getUserById(currentUser.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Provision new user", description = "Admin endpoint to create a new user account within the tenant")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = userService.createUser(currentUser.getTenantId(), request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User provisioned successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Update user profile", description = "Updates user full name, job title, or department")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        // Enforce that Employees can only update their own profile
        if (!currentUser.getId().equals(id) && !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Forbidden: You cannot modify another user's profile.");
        }

        UserResponse response = userService.updateUser(currentUser.getTenantId(), id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate user account", description = "Admin endpoint to enable/disable user login access")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = userService.updateUserStatus(currentUser.getTenantId(), id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user account", description = "Admin endpoint to remove user from tenant")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        userService.deleteUser(currentUser.getTenantId(), id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user", description = "Admin endpoint to assign a role to a target user")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoleToUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID userId,
            @PathVariable UUID roleId
    ) {
        UserResponse response = userService.assignRoleToUser(currentUser.getTenantId(), userId, roleId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", response));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role from user", description = "Admin endpoint to revoke a role from a target user")
    public ResponseEntity<ApiResponse<UserResponse>> removeRoleFromUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID userId,
            @PathVariable UUID roleId
    ) {
        UserResponse response = userService.removeRoleFromUser(currentUser.getTenantId(), userId, roleId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", response));
    }
}
