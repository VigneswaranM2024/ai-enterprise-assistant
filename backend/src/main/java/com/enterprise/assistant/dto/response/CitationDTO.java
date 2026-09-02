package com.enterprise.assistant.dto.response;

import java.util.UUID;

/**
 * Source citation metadata referencing an authoritative document chunk matched during retrieval.
 */
public record CitationDTO(
    String citationId,
    UUID documentId,
    UUID chunkId,
    String documentTitle,
    String fileName,
    Integer chunkIndex,
    Double similarityScore
) {}
