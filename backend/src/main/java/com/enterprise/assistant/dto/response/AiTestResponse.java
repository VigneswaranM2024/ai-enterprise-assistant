package com.enterprise.assistant.dto.response;

/**
 * Response payload for temporary AI test endpoint.
 */
public record AiTestResponse(
    String response,
    String model
) {}
