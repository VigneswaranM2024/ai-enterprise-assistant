package com.enterprise.assistant.dto.ai.groq;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Chat completion request payload for Groq OpenAI-compatible endpoint.
 */
public record GroqChatRequest(
    String model,
    List<GroqMessage> messages,
    double temperature,
    @JsonProperty("max_tokens") int maxTokens
) {}
