package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO record for Admin creation/provisioning of a new user.
 */
public record CreateUserRequest(
    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password,

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    String fullName,

    String jobTitle,

    UUID departmentId,

    String securityClassification, // E.g. PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED, TOP_SECRET

    @NotEmpty(message = "At least one role must be assigned")
    Set<String> roleNames
) {}
