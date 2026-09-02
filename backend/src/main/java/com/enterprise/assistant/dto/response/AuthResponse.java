package com.enterprise.assistant.dto.response;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO record returning authentication tokens and user profile metadata.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserProfileResponse user
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserProfileResponse user) {
        this(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
