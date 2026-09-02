package com.enterprise.assistant.service.audit;

import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.User;

import java.util.UUID;

/**
 * Service Contract for Recording Audit Events.
 */
public interface AuditLogService {

    void logEvent(
        Tenant tenant,
        User actor,
        String action,
        String targetResourceType,
        UUID targetResourceId,
        String payloadJson,
        String status
    );
}
