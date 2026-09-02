package com.enterprise.assistant.eval.pipeline;

import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.document.DocumentCategory;
import com.enterprise.assistant.domain.document.DocumentStatus;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration & Lifecycle Tests for the Document Ingestion Pipeline:
 * UPLOAD -> VALIDATE -> STORE -> EXTRACT -> NORMALIZE -> CHUNK -> EMBED -> SAVE -> READY / FAILED
 */
@ExtendWith(MockitoExtension.class)
class DocumentPipelineIntegrationTest {

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
    private UUID uploaderId;
    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        uploaderId = UUID.randomUUID();
        testTenant = Tenant.builder().id(tenantId).name("Pipeline Tenant").build();
        testUser = User.builder().id(uploaderId).tenant(testTenant).email("uploader@tenant.com").build();

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

        org.springframework.test.util.ReflectionTestUtils.setField(documentService, "targetTokens", 1000);
        org.springframework.test.util.ReflectionTestUtils.setField(documentService, "overlapTokens", 150);
    }

    @Test
    void documentUpload_CompleteLifecycleToReadyStatus() throws Exception {
        byte[] pdfMagicBytes = "%PDF-1.5 Sample PDF text content for ingestion test".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", pdfMagicBytes);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(uploaderId)).thenReturn(Optional.of(testUser));
        when(documentStorageService.store(eq(tenantId), any(UUID.class), eq(file))).thenReturn("file://storage/policy.pdf");

        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Resource resource = new ByteArrayResource(pdfMagicBytes);
        when(documentStorageService.retrieve("file://storage/policy.pdf")).thenReturn(resource);

        when(documentTextExtractor.extractText(any(), eq("application/pdf"))).thenReturn("Extracted policy text content.");
        when(textNormalizer.normalize("Extracted policy text content.")).thenReturn("Extracted policy text content.");

        com.enterprise.assistant.domain.document.DocumentChunk chunk = com.enterprise.assistant.domain.document.DocumentChunk.builder()
                .id(UUID.randomUUID())
                .tenant(testTenant)
                .content("Extracted policy text content.")
                .chunkIndex(0)
                .characterCount(30)
                .tokenEstimate(6)
                .securityClassification(SecurityClassification.INTERNAL)
                .build();

        when(documentChunker.chunk(any(), any())).thenReturn(List.of(chunk));
        when(documentChunkRepository.saveAll(anyList())).thenReturn(List.of(chunk));

        when(embeddingService.generateEmbeddings(anyList(), any())).thenReturn(List.of(List.of(0.1f, 0.2f)));

        DocumentResponse resp = documentService.uploadDocument(
                tenantId, uploaderId, file, "Leave Policy 2026", "POLICY", "INTERNAL", new String[]{"ROLE_EMPLOYEE"}, new String[]{"hr"}
        );

        assertNotNull(resp);
        assertEquals("READY", resp.status());
        assertEquals("Leave Policy 2026", resp.title());

        verify(documentRepository, atLeastOnce()).save(argThat(d -> d.getStatus() == DocumentStatus.READY));
    }

    @Test
    void documentUpload_ExtractionFailure_TransitionsToFailedStatus() throws Exception {
        byte[] pdfMagicBytes = "%PDF-1.5 Corrupted content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "corrupt.pdf", "application/pdf", pdfMagicBytes);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(uploaderId)).thenReturn(Optional.of(testUser));
        when(documentStorageService.store(eq(tenantId), any(UUID.class), eq(file))).thenReturn("file://storage/corrupt.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Resource resource = new ByteArrayResource(pdfMagicBytes);
        when(documentStorageService.retrieve("file://storage/corrupt.pdf")).thenReturn(resource);

        when(documentTextExtractor.extractText(any(), eq("application/pdf"))).thenThrow(new RuntimeException("Tika PDF parsing failed"));

        DocumentResponse resp = documentService.uploadDocument(
                tenantId, uploaderId, file, "Corrupt PDF", "GENERAL", "INTERNAL", null, null
        );

        assertNotNull(resp);
        assertEquals("FAILED", resp.status());

        verify(documentRepository, atLeastOnce()).save(argThat(d -> d.getStatus() == DocumentStatus.FAILED && d.getErrorMessage().contains("Tika PDF parsing failed")));
    }

    @Test
    void documentUpload_EmptyFile_ThrowsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                documentService.uploadDocument(tenantId, uploaderId, emptyFile, "Empty", "GENERAL", "INTERNAL", null, null));
    }
}
