package com.enterprise.assistant.dto.response;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable DTO Record for User Profile metadata.
 */
public record UserDTO(
    UUID id,
    UUID tenantId,
    String tenantSlug,
    String email,
    String fullName,
    String jobTitle,
    String securityClassification,
    Set<String> roles
) {}
