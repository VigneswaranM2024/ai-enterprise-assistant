package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Immutable DTO record for creating a new custom role within a tenant.
 */
public record CreateRoleRequest(
    @NotBlank(message = "Role name is required")
    @Size(min = 3, max = 50, message = "Role name must be between 3 and 50 characters")
    String name,

    String description,

    Set<String> permissionCodes
) {}
