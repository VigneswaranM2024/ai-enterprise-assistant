package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Immutable DTO Record for updating user active status.
 */
public record UpdateUserStatusRequest(
    @NotNull(message = "Active status is required")
    Boolean isActive
) {}
