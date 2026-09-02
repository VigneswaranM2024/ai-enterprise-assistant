package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO record returning role details and permissions.
 */
public record RoleResponse(
    UUID id,
    UUID tenantId,
    String name,
    String description,
    Boolean isSystemRole,
    Set<String> permissions,
    OffsetDateTime createdAt
) {}
