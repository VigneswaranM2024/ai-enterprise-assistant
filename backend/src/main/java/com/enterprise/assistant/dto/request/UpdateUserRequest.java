package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Immutable DTO Record for updating non-security user profile attributes.
 */
public record UpdateUserRequest(
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    String fullName,

    @Size(max = 100, message = "Job title cannot exceed 100 characters")
    String jobTitle,

    UUID departmentId
) {}
