package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for RAG chat generation endpoint.
 */
public record RagRequest(
    @NotBlank(message = "Query must not be empty")
    @Size(max = 2000, message = "Query length must not exceed 2000 characters")
    String query,

    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 10, message = "topK must not exceed 10")
    Integer topK
) {
    public int getEffectiveTopK(int defaultTopK, int maxTopK) {
        int k = topK != null ? topK : defaultTopK;
        return Math.min(Math.max(1, k), maxTopK);
    }
}
