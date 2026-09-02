package com.enterprise.assistant.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight DTO record returning Document summary details.
 */
public record DocumentSummaryResponse(
    UUID id,
    String title,
    String category,
    String mimeType,
    Long fileSizeBytes,
    String status,
    String uploaderName,
    OffsetDateTime createdAt
) {}
