package com.enterprise.assistant.repository.document;

import com.enterprise.assistant.domain.document.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for DocumentChunk entities with pgvector similarity search support.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByTenantIdAndDocumentIdOrderByChunkIndexAsc(UUID tenantId, UUID documentId);

    void deleteByTenantIdAndDocumentId(UUID tenantId, UUID documentId);

    long countByTenantIdAndDocumentId(UUID tenantId, UUID documentId);

    long countByTenantId(UUID tenantId);

    @Modifying
    @Query(value = "UPDATE document_chunks SET embedding = CAST(:embeddingStr AS vector) WHERE id = :chunkId AND tenant_id = :tenantId", nativeQuery = true)
    void updateChunkEmbedding(@Param("chunkId") UUID chunkId, @Param("tenantId") UUID tenantId, @Param("embeddingStr") String embeddingStr);

    /**
     * Tenant-only similarity search (used by the public /search/semantic endpoint).
     * Does NOT enforce role, department, or classification filtering.
     */
    @Query(value = """
        SELECT 
            dc.id AS chunk_id,
            dc.document_id AS document_id,
            dc.content AS content,
            1 - (dc.embedding <=> CAST(:queryVector AS vector)) AS similarity_score,
            dc.metadata AS metadata
        FROM document_chunks dc
        WHERE dc.tenant_id = :tenantId
          AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <=> CAST(:queryVector AS vector) ASC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> findSimilarChunksNative(
        @Param("tenantId") UUID tenantId,
        @Param("queryVector") String queryVector,
        @Param("topK") int topK
    );

    /**
     * Authorized similarity search enforcing tenant isolation, security classification,
     * role-based access, and department-based access inside the database query.
     *
     * <p>The actual SQL is defined as a {@code @NamedNativeQuery} on {@link com.enterprise.assistant.domain.document.DocumentChunk}
     * to avoid Spring Data JPA's inline query validator choking on the pgvector {@code <=>} operator.</p>
     *
     * @param tenantId         Tenant UUID from JWT — never from client body
     * @param queryVector      pgvector-formatted embedding string
     * @param topK             Maximum results to return
     * @param userClearance    User's SecurityClassification name (e.g. "CONFIDENTIAL")
     * @param userRoles        PostgreSQL text array literal of role names (e.g. "{ROLE_ADMIN,ROLE_EMPLOYEE}")
     * @param userDeptCode     User's department code, or null if none assigned
     */
    @Query(value = """
        SELECT 
            dc.id           AS chunk_id,
            dc.document_id  AS document_id,
            dc.content      AS content,
            1 - (dc.embedding <=> CAST(:queryVector AS vector)) AS similarity_score,
            dc.metadata     AS metadata
        FROM document_chunks dc
        WHERE dc.tenant_id = :tenantId
          AND dc.embedding IS NOT NULL
          AND ARRAY_POSITION(
                ARRAY['PUBLIC','INTERNAL','CONFIDENTIAL','RESTRICTED','TOP_SECRET'],
                dc.security_classification::text
              )
              <=
              ARRAY_POSITION(
                ARRAY['PUBLIC','INTERNAL','CONFIDENTIAL','RESTRICTED','TOP_SECRET'],
                CAST(:userClearance AS text)
              )
          AND (
                dc.allowed_roles IS NULL
                OR array_length(dc.allowed_roles, 1) IS NULL
                OR dc.allowed_roles && CAST(:userRoles AS text[])
              )
          AND (
                dc.allowed_departments IS NULL
                OR array_length(dc.allowed_departments, 1) IS NULL
                OR (:userDeptCode IS NOT NULL AND :userDeptCode = ANY(dc.allowed_departments))
              )
        ORDER BY dc.embedding <=> CAST(:queryVector AS vector) ASC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> findSimilarChunksAuthorized(
        @Param("tenantId")      UUID tenantId,
        @Param("queryVector")   String queryVector,
        @Param("topK")          int topK,
        @Param("userClearance") String userClearance,
        @Param("userRoles")     String userRoles,
        @Param("userDeptCode")  String userDeptCode
    );
}
