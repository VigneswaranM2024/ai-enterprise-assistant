package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new ChatSession.
 */
public record CreateSessionRequest(
    @Size(max = 150, message = "Session title cannot exceed 150 characters")
    String title
) {
    public String getEffectiveTitle() {
        return (title != null && !title.isBlank()) ? title.trim() : "New Chat";
    }
}
