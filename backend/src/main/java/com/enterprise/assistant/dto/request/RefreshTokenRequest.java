package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable DTO record for Refresh Token Rotation request.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
