package com.enterprise.assistant.dto.response;

import java.util.Set;
import java.util.UUID;

/**
 * Compact DTO Record for User List views.
 */
public record UserSummaryResponse(
    UUID id,
    String email,
    String fullName,
    String jobTitle,
    Boolean isActive,
    Set<String> roles
) {}
