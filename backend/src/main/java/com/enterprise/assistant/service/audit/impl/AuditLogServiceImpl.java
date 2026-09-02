package com.enterprise.assistant.service.audit.impl;

import com.enterprise.assistant.domain.audit.AuditLog;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.repository.audit.AuditLogRepository;
import com.enterprise.assistant.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of AuditLogService ensuring audit records are saved reliably.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(
            Tenant tenant,
            User actor,
            String action,
            String targetResourceType,
            UUID targetResourceId,
            String payloadJson,
            String status
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenant(tenant)
                    .actorUser(actor)
                    .action(action)
                    .targetResourceType(targetResourceType)
                    .targetResourceId(targetResourceId)
                    .payload(payloadJson)
                    .status(status != null ? status : "SUCCESS")
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Recorded audit event: '{}' on resource: '{}' ({})", action, targetResourceType, targetResourceId);
        } catch (Exception ex) {
            log.error("Failed to record audit event: '{}'", action, ex);
        }
    }
}
