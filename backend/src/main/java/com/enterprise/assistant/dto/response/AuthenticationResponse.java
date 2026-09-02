package com.enterprise.assistant.dto.response;

import java.util.UUID;

/**
 * Immutable DTO Record for Authentication tokens response.
 */
public record AuthenticationResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserDTO user
) {
    public AuthenticationResponse(String accessToken, String refreshToken, long expiresInSeconds, UserDTO user) {
        this(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
