package com.enterprise.assistant.dto.response;

import java.util.UUID;

/**
 * Result item DTO representing a matched DocumentChunk in semantic search.
 */
public record SearchResultItemResponse(
    UUID chunkId,
    UUID documentId,
    String content,
    Double score,
    String metadata
) {}
