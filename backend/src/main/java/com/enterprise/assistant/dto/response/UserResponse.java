package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO Record for returning full User details.
 * Passwords, hashes, and JWT secrets are strictly excluded.
 */
public record UserResponse(
    UUID id,
    UUID tenantId,
    String tenantSlug,
    String email,
    String fullName,
    String jobTitle,
    UUID departmentId,
    String departmentName,
    String securityClassification,
    Boolean isActive,
    OffsetDateTime lastLoginAt,
    Set<String> roles,
    Set<String> permissions,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
