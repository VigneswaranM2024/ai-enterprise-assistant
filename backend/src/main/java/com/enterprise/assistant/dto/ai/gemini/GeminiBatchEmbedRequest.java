package com.enterprise.assistant.dto.ai.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request payload structure for Gemini batchEmbedContents API endpoint.
 */
public record GeminiBatchEmbedRequest(
    List<EmbedContentRequest> requests
) {
    public record EmbedContentRequest(
        String model,
        Content content,
        String taskType,
        @JsonProperty("outputDimensionality") Integer outputDimensionality
    ) {}

    public record Content(
        List<Part> parts
    ) {}

    public record Part(
        String text
    ) {}
}
