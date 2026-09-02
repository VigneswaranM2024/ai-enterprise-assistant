package com.enterprise.assistant.repository.document;

import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.document.DocumentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Document Entity.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    boolean existsByTenantIdAndChecksum(UUID tenantId, String checksum);

    Optional<Document> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT d FROM Document d WHERE d.tenant.id = :tenantId AND " +
           "(:query = '' OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:query as string), '%'))) AND " +
           "(:category IS NULL OR d.category = :category)")
    Page<Document> searchTenantDocuments(@Param("tenantId") UUID tenantId,
                                         @Param("query") String query,
                                         @Param("category") DocumentCategory category,
                                         Pageable pageable);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, com.enterprise.assistant.domain.document.DocumentStatus status);

    @Query("SELECT COALESCE(SUM(d.fileSizeBytes), 0) FROM Document d WHERE d.tenant.id = :tenantId")
    long sumFileSizeBytesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d.category, COUNT(d) FROM Document d WHERE d.tenant.id = :tenantId GROUP BY d.category")
    java.util.List<Object[]> countByCategoryByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(d.fileSizeBytes), 0) FROM Document d")
    long sumAllFileSizeBytes();
}
