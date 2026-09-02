package com.enterprise.assistant.dto.ai.gemini;

import java.util.List;

/**
 * Response payload structure for Gemini batchEmbedContents API endpoint.
 */
public record GeminiBatchEmbedResponse(
    List<EmbeddingData> embeddings
) {
    public record EmbeddingData(
        List<Float> values
    ) {}
}
