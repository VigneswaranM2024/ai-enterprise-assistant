package com.enterprise.assistant.dto.ai.groq;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Chat completion response payload returned by Groq OpenAI-compatible endpoint.
 */
public record GroqChatResponse(
    String id,
    List<Choice> choices
) {
    public record Choice(
        int index,
        GroqMessage message,
        @JsonProperty("finish_reason") String finishReason
    ) {}
}
