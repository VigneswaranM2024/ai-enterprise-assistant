package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for temporary AI test endpoint.
 */
public record AiTestRequest(
    @NotBlank(message = "Prompt must not be empty") String prompt
) {}
