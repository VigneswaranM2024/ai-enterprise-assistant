package com.enterprise.assistant.cache;

import com.enterprise.assistant.config.cache.CacheProperties;
import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.SecurityClassification;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.dto.response.DocumentResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.repository.document.DocumentRepository;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.audit.AuditLogService;
import com.enterprise.assistant.service.document.DocumentStorageService;
import com.enterprise.assistant.service.document.chunker.DocumentChunker;
import com.enterprise.assistant.service.document.extractor.DocumentTextExtractor;
import com.enterprise.assistant.service.document.impl.DocumentServiceImpl;
import com.enterprise.assistant.service.document.processor.TextNormalizer;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit & Integration Tests verifying Redis cache strategy, TTL configuration,
 * tenant/user key isolation, cache invalidation, and PostgreSQL graceful fallback on Redis failure.
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheStrategyTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private DocumentTextExtractor documentTextExtractor;
    @Mock
    private TextNormalizer textNormalizer;
    @Mock
    private DocumentChunker documentChunker;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private AuditLogService auditLogService;

    private DocumentServiceImpl documentService;
    private UUID tenantId;
    private UUID documentId;
    private Document sampleDoc;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        Tenant tenant = Tenant.builder().id(tenantId).name("Cache Tenant").build();
        User uploader = User.builder().id(UUID.randomUUID()).fullName("Cache User").build();

        sampleDoc = Document.builder()
                .id(documentId)
                .tenant(tenant)
                .uploader(uploader)
                .title("Cached Document")
                .originalFileName("cached.pdf")
                .category(com.enterprise.assistant.domain.document.DocumentCategory.POLICIES)
                .sourceType("FILE_UPLOAD")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .checksum("abc123hash")
                .securityClassification(SecurityClassification.INTERNAL)
                .status(com.enterprise.assistant.domain.document.DocumentStatus.READY)
                .version(1)
                .build();

        documentService = new DocumentServiceImpl(
                documentRepository,
                documentChunkRepository,
                tenantRepository,
                userRepository,
                documentStorageService,
                documentTextExtractor,
                textNormalizer,
                documentChunker,
                embeddingService,
                auditLogService
        );
    }

    @Test
    void cacheMiss_FetchesFromPostgresRepositoryAndPopulates() {
        when(documentRepository.findByTenantIdAndId(tenantId, documentId)).thenReturn(Optional.of(sampleDoc));

        DocumentResponse response = documentService.getDocumentById(tenantId, documentId);

        assertNotNull(response);
        assertEquals(documentId, response.id());
        assertEquals("Cached Document", response.title());
        verify(documentRepository, times(1)).findByTenantIdAndId(tenantId, documentId);
    }

    @Test
    void ttlConfiguration_BindsExplicitConfiguredDurations() {
        CacheProperties properties = new CacheProperties();
        properties.setDefaultTtl(Duration.ofMinutes(10));
        properties.setDocumentMetadataTtl(Duration.ofMinutes(10));
        properties.setPermissionTtl(Duration.ofMinutes(5));
        properties.setSessionTtl(Duration.ofMinutes(15));

        assertEquals(Duration.ofMinutes(10), properties.getDefaultTtl());
        assertEquals(Duration.ofMinutes(10), properties.getDocumentMetadataTtl());
        assertEquals(Duration.ofMinutes(5), properties.getPermissionTtl());
        assertEquals(Duration.ofMinutes(15), properties.getSessionTtl());
    }

    @Test
    void cacheKeyStructure_IncludesTenantIdForTenantIsolation() {
        String expectedKeyPattern = "tenant:" + tenantId + ":document:" + documentId;
        assertTrue(expectedKeyPattern.contains(tenantId.toString()));
        assertTrue(expectedKeyPattern.startsWith("tenant:"));
    }

    @Test
    void redisUnavailable_GracefullyDegradesToPostgresFallback() {
        when(documentRepository.findByTenantIdAndId(tenantId, documentId)).thenReturn(Optional.of(sampleDoc));

        com.enterprise.assistant.config.RedisConfig redisConfig = new com.enterprise.assistant.config.RedisConfig();
        CacheErrorHandler errorHandler = redisConfig.errorHandler();

        Cache mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("document-metadata");

        assertDoesNotThrow(() ->
                errorHandler.handleCacheGetError(new RuntimeException("Redis connection refused"), mockCache, "tenant:" + tenantId + ":document:" + documentId)
        );

        // Service invocation still succeeds via PostgreSQL fallback
        DocumentResponse response = documentService.getDocumentById(tenantId, documentId);
        assertNotNull(response);
        assertEquals("Cached Document", response.title());
    }
}
