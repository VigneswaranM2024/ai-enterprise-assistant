package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for semantic similarity search.
 */
public record SemanticSearchRequest(
    @NotBlank(message = "Search query must not be empty") String query,
    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 50, message = "topK must not exceed 50")
    Integer topK
) {
    public int getEffectiveTopK() {
        return topK != null ? topK : 5;
    }
}
