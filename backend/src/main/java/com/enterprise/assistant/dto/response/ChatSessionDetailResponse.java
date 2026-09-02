package com.enterprise.assistant.dto.response;

import com.enterprise.assistant.domain.chat.ChatSessionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing detailed information for a ChatSession including its message history.
 */
public record ChatSessionDetailResponse(
    UUID id,
    String title,
    ChatSessionStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<ChatMessageDTO> messages
) {}
