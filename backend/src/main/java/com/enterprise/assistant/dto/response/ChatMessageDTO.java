package com.enterprise.assistant.dto.response;

import com.enterprise.assistant.domain.chat.MessageRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing an individual ChatMessage turn.
 */
public record ChatMessageDTO(
    UUID id,
    MessageRole role,
    String content,
    List<CitationDTO> citations,
    OffsetDateTime createdAt
) {}
