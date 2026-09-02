package com.enterprise.assistant.repository.audit;

import com.enterprise.assistant.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA Repository for AuditLog Entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenant.id = :tenantId AND a.createdAt >= :since")
    long countByTenantIdAndCreatedAtAfter(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId, @org.springframework.data.repository.query.Param("since") java.time.OffsetDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenant.id = :tenantId AND a.status = :status AND a.createdAt >= :since")
    long countByTenantIdAndStatusAndCreatedAtAfter(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId, @org.springframework.data.repository.query.Param("status") String status, @org.springframework.data.repository.query.Param("since") java.time.OffsetDateTime since);
}
