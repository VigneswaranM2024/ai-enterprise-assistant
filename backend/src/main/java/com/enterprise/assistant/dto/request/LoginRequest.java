package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable DTO record for user login authentication request.
 */
public record LoginRequest(
    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password,

    @NotBlank(message = "Tenant slug is required")
    String tenantSlug
) {}
