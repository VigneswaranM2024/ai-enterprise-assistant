package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for sending a message turn within a ChatSession.
 */
public record SendMessageRequest(
    @NotBlank(message = "Message content must not be blank")
    @Size(max = 4000, message = "Message content cannot exceed 4000 characters")
    String message
) {}
