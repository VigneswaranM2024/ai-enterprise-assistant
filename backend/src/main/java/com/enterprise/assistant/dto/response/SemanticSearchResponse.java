package com.enterprise.assistant.dto.response;

import java.util.List;

/**
 * Top-K semantic similarity search response DTO.
 */
public record SemanticSearchResponse(
    String query,
    int totalResults,
    List<SearchResultItemResponse> results
) {}
