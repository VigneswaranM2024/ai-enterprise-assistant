package com.enterprise.assistant.dto.response;

import com.enterprise.assistant.domain.chat.ChatSessionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO representing summary information for a ChatSession.
 */
public record ChatSessionResponse(
    UUID id,
    String title,
    ChatSessionStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
