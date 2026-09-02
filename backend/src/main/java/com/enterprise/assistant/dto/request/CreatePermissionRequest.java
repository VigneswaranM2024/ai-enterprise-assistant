package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable DTO Record for creating a permission.
 */
public record CreatePermissionRequest(
    @NotBlank(message = "Permission code is required")
    @Size(min = 3, max = 100, message = "Code must be between 3 and 100 characters")
    String code,

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
    String category,

    String description
) {}
