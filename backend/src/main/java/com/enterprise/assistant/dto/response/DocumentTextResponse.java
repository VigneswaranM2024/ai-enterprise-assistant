package com.enterprise.assistant.dto.response;

import java.util.UUID;

/**
 * Immutable DTO record returning extracted text content for a document.
 */
public record DocumentTextResponse(
    UUID id,
    UUID tenantId,
    String title,
    String status,
    String extractedText
) {}
