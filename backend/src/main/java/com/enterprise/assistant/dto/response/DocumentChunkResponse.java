package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable DTO record returning DocumentChunk details for debug/admin inspection.
 */
public record DocumentChunkResponse(
    UUID id,
    UUID tenantId,
    UUID documentId,
    Integer chunkIndex,
    String content,
    Integer characterCount,
    Integer tokenEstimate,
    Integer pageNumber,
    String metadata,
    String securityClassification,
    OffsetDateTime createdAt
) {}
