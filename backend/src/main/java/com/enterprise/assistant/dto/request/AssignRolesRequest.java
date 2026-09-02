package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * Immutable DTO record for assigning roles to a user.
 */
public record AssignRolesRequest(
    @NotEmpty(message = "Role names set cannot be empty")
    Set<String> roleNames
) {}
