package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable DTO record returning Document details.
 */
public record DocumentResponse(
    UUID id,
    UUID tenantId,
    String title,
    String originalFileName,
    String description,
    String category,
    String sourceType,
    String mimeType,
    Long fileSizeBytes,
    String checksum,
    String securityClassification,
    String[] allowedRoles,
    String[] allowedDepartments,
    String[] tags,
    String status,
    Integer version,
    String uploaderName,
    OffsetDateTime createdAt
) {}
