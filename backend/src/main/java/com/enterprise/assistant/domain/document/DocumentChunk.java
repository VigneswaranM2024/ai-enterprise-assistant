package com.enterprise.assistant.domain.document;

import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.SecurityClassification;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;



/**
 * DocumentChunk Domain Entity.
 * Represents a semantic chunk of an ingested document for vector storage and RAG retrieval.
 *
 * <p>The {@code @NamedNativeQuery} above registers the authorized similarity-search query
 * at the entity level to bypass Spring Data JPA's inline {@code @Query} validator, which
 * cannot parse the pgvector {@code <=>} operator in a string literal.</p>
 */
@Entity
@Table(
    name = "document_chunks",
    indexes = {
        @Index(name = "idx_chunks_doc_id", columnList = "document_id"),
        @Index(name = "idx_chunks_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_chunks_doc_index", columnList = "document_id, chunk_index")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_doc_chunk_index", columnNames = {"document_id", "chunk_index"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    @Column(name = "token_count", nullable = false)
    private Integer tokenEstimate;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "security_classification", nullable = false, length = 30)
    @Builder.Default
    private SecurityClassification securityClassification = SecurityClassification.INTERNAL;

    @Column(name = "allowed_roles", columnDefinition = "text[]")
    private String[] allowedRoles;

    @Column(name = "allowed_departments", columnDefinition = "text[]")
    private String[] allowedDepartments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
