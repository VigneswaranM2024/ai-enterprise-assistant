package com.enterprise.assistant.dto.response;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO record returning user details.
 */
public record UserProfileResponse(
    UUID id,
    UUID tenantId,
    String tenantSlug,
    String email,
    String fullName,
    String jobTitle,
    String securityClassification,
    Set<String> roles
) {}
