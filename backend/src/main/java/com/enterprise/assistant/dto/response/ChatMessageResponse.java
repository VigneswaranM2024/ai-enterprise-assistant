package com.enterprise.assistant.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO returned after sending a new message in a ChatSession.
 */
public record ChatMessageResponse(
    UUID sessionId,
    ChatMessageDTO userMessage,
    ChatMessageDTO assistantMessage,
    List<CitationDTO> citations,
    int sourcesUsed,
    String model
) {}
