package com.enterprise.assistant.dto.request;

import java.util.Set;

/**
 * Immutable DTO record for updating an existing role within a tenant.
 */
public record UpdateRoleRequest(
    String description,

    Set<String> permissionCodes
) {}
