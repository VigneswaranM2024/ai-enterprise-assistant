package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable DTO Record for Permission details.
 */
public record PermissionResponse(
    UUID id,
    String code,
    String category,
    String description,
    OffsetDateTime createdAt
) {}
