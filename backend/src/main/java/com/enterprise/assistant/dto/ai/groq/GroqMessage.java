package com.enterprise.assistant.dto.ai.groq;

/**
 * Message record representing role and content in Groq chat completions API.
 */
public record GroqMessage(
    String role,
    String content
) {}
