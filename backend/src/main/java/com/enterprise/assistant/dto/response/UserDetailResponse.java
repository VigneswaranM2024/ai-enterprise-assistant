package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO record returning full user profile details.
 */
public record UserDetailResponse(
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
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
