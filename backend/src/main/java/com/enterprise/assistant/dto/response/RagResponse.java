package com.enterprise.assistant.dto.response;

import java.util.List;

/**
 * Structured response containing synthesized answer text, verified citations, and metadata.
 */
public record RagResponse(
    String answer,
    List<CitationDTO> citations,
    int sourcesUsed,
    String model
) {}
