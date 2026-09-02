package com.enterprise.assistant.service.document.impl;

import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.document.DocumentCategory;
import com.enterprise.assistant.domain.document.DocumentChunk;
import com.enterprise.assistant.domain.document.DocumentStatus;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.SecurityClassification;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.dto.request.DocumentUpdateRequest;
import com.enterprise.assistant.dto.response.*;
import com.enterprise.assistant.exception.ResourceNotFoundException;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.repository.document.DocumentRepository;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.audit.AuditLogService;
import com.enterprise.assistant.service.document.DocumentService;
import com.enterprise.assistant.service.document.DocumentStorageService;
import com.enterprise.assistant.service.document.chunker.ChunkingOptions;
import com.enterprise.assistant.service.document.chunker.DocumentChunker;
import com.enterprise.assistant.service.document.extractor.DocumentTextExtractor;
import com.enterprise.assistant.service.document.processor.TextNormalizer;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
import com.enterprise.assistant.util.FileHashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentService managing document metadata, physical storage,
 * text extraction, semantic chunking, and vector embedding pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentTextExtractor documentTextExtractor;
    private final TextNormalizer textNormalizer;
    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final AuditLogService auditLogService;

    @Value("${document.chunking.target-tokens:1000}")
    private int targetTokens;

    @Value("${document.chunking.overlap-tokens:150}")
    private int overlapTokens;

    @Override
    @Transactional
    public DocumentResponse uploadDocument(
            UUID tenantId,
            UUID uploaderId,
            MultipartFile file,
            String title,
            String categoryStr,
            String securityClassificationStr,
            String[] allowedRoles,
            String[] tags
    ) {
        log.info("Uploading document for tenant: {}", tenantId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        validateFileType(file);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + uploaderId));

        String checksum = FileHashUtils.calculateChecksum(file);

        DocumentCategory category = DocumentCategory.GENERAL;
        if (categoryStr != null && !categoryStr.isBlank()) {
            try {
                category = DocumentCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid document category: {}, falling back to GENERAL", categoryStr);
            }
        }

        SecurityClassification classification = SecurityClassification.INTERNAL;
        if (securityClassificationStr != null && !securityClassificationStr.isBlank()) {
            try {
                classification = SecurityClassification.valueOf(securityClassificationStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid security classification: {}", securityClassificationStr);
            }
        }

        UUID documentId = UUID.randomUUID();
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        // 1. Store physical file
        String fileUri = documentStorageService.store(tenantId, documentId, file);

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        Document document = Document.builder()
                .id(documentId)
                .tenant(tenant)
                .uploader(uploader)
                .title(title != null && !title.isBlank() ? title : originalFileName)
                .originalFileName(originalFileName)
                .category(category)
                .sourceType("FILE_UPLOAD")
                .sourceUri(fileUri)
                .mimeType(mimeType)
                .fileSizeBytes(file.getSize())
                .checksum(checksum)
                .securityClassification(classification)
                .allowedRoles(allowedRoles != null ? allowedRoles : new String[]{"ROLE_EMPLOYEE", "ROLE_ADMIN"})
                .tags(tags != null ? tags : new String[]{})
                .status(DocumentStatus.PROCESSING)
                .version(1)
                .build();

        Document savedDoc;
        try {
            savedDoc = documentRepository.save(document);
            log.info("Document metadata initialized in PROCESSING state (ID: {})", savedDoc.getId());
            auditLogService.logEvent(tenant, uploader, "DOCUMENT_UPLOADED", "DOCUMENT", savedDoc.getId(), "{\"fileName\":\"" + savedDoc.getOriginalFileName() + "\"}", "SUCCESS");
            auditLogService.logEvent(tenant, uploader, "DOCUMENT_PROCESSING_STARTED", "DOCUMENT", savedDoc.getId(), "{}", "SUCCESS");
        } catch (Exception ex) {
            log.error("Database save failed for document {}. Cleaning up physical file.", documentId, ex);
            documentStorageService.delete(fileUri);
            throw new RuntimeException("Failed to save document metadata", ex);
        }

        // 2. Execute Text Extraction, Semantic Chunking, & Vector Embedding Pipeline
        processDocumentExtractionAndChunking(savedDoc, uploader);

        return mapToDocumentResponse(savedDoc);
    }

    private void processDocumentExtractionAndChunking(Document document, User actor) {
        try {
            // A. Text Extraction & Normalization
            Resource fileResource = documentStorageService.retrieve(document.getSourceUri());
            if (!fileResource.exists()) {
                throw new IOException("Physical document file missing at storage key");
            }

            String rawText = documentTextExtractor.extractText(fileResource, document.getMimeType());
            String normalizedText = textNormalizer.normalize(rawText);

            if (normalizedText.isBlank()) {
                throw new IllegalStateException("Extracted text content is empty or unreadable");
            }

            document.setExtractedText(normalizedText);

            // B. Semantic Chunking
            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_CHUNKING_STARTED", "DOCUMENT", document.getId(), "{}", "SUCCESS");

            // Clean previous chunks for version safety
            documentChunkRepository.deleteByTenantIdAndDocumentId(document.getTenant().getId(), document.getId());

            ChunkingOptions options = new ChunkingOptions(targetTokens, overlapTokens);
            List<DocumentChunk> chunks = documentChunker.chunk(document, options);

            if (chunks.isEmpty()) {
                throw new IllegalStateException("Failed to generate valid chunks from document text");
            }

            List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunks);
            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_CHUNKING_COMPLETED", "DOCUMENT", document.getId(), "{\"chunksCount\":" + savedChunks.size() + "}", "SUCCESS");

            // C. Generate & Store Vector Embeddings
            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_EMBEDDING_STARTED", "DOCUMENT", document.getId(), "{}", "SUCCESS");
            List<String> chunkTexts = savedChunks.stream().map(DocumentChunk::getContent).collect(Collectors.toList());

            List<List<Float>> embeddings = embeddingService.generateEmbeddings(chunkTexts, GeminiTaskType.RETRIEVAL_DOCUMENT);
            if (embeddings == null || embeddings.size() != savedChunks.size()) {
                throw new IllegalStateException("Generated vector embeddings count mismatch for document chunks");
            }

            for (int i = 0; i < savedChunks.size(); i++) {
                DocumentChunk chunk = savedChunks.get(i);
                List<Float> vector = embeddings.get(i);
                String vectorStr = formatVectorToString(vector);
                documentChunkRepository.updateChunkEmbedding(chunk.getId(), document.getTenant().getId(), vectorStr);
            }

            // D. Finalize Status
            document.setStatus(DocumentStatus.READY);
            document.setErrorMessage(null);
            documentRepository.save(document);

            log.info("Document processing, chunking & embedding completed successfully (ID: {}, Chunks/Embeddings: {})", document.getId(), savedChunks.size());
            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_EMBEDDING_COMPLETED", "DOCUMENT", document.getId(), "{\"embeddingsCount\":" + savedChunks.size() + "}", "SUCCESS");
            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_PROCESSING_COMPLETED", "DOCUMENT", document.getId(), "{\"chunksCount\":" + savedChunks.size() + "}", "SUCCESS");
        } catch (Exception ex) {
            log.error("Document processing, chunking or embedding pipeline failed (ID: {})", document.getId(), ex);

            // Safely rollback any partial chunk/embedding generation
            try {
                documentChunkRepository.deleteByTenantIdAndDocumentId(document.getTenant().getId(), document.getId());
            } catch (Exception rollbackEx) {
                log.warn("Failed to clean up partial chunks for document: {}", document.getId(), rollbackEx);
            }

            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage("Processing failed: " + ex.getMessage());
            documentRepository.save(document);

            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_PROCESSING_FAILED", "DOCUMENT", document.getId(), "{\"error\":\"" + ex.getMessage() + "\"}", "FAILURE");
            auditLogService.logEvent(document.getTenant(), actor, "DOCUMENT_EMBEDDING_FAILED", "DOCUMENT", document.getId(), "{\"error\":\"" + ex.getMessage() + "\"}", "FAILURE");
        }
    }

    private String formatVectorToString(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            sb.append(vector.get(i));
            if (i < vector.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentSummaryResponse> getDocuments(UUID tenantId, String query, String categoryStr, Pageable pageable) {
        DocumentCategory category = null;
        if (categoryStr != null && !categoryStr.isBlank()) {
            try {
                category = DocumentCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid filter category: {}", categoryStr);
            }
        }

        Page<Document> page = documentRepository.searchTenantDocuments(tenantId, query, category, pageable);
        Page<DocumentSummaryResponse> responsePage = page.map(this::mapToDocumentSummaryResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "document-metadata", key = "'tenant:' + #tenantId + ':document:' + #documentId", unless = "#result == null", sync = true)
    public DocumentResponse getDocumentById(UUID tenantId, UUID documentId) {
        Document document = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        return mapToDocumentResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocument(UUID tenantId, UUID documentId, UUID downloaderId) {
        Document document = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        User downloader = downloaderId != null ? userRepository.findById(downloaderId).orElse(null) : null;
        auditLogService.logEvent(document.getTenant(), downloader, "DOCUMENT_DOWNLOADED", "DOCUMENT", document.getId(), "{}", "SUCCESS");

        return documentStorageService.retrieve(document.getSourceUri());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentTextResponse getDocumentText(UUID tenantId, UUID documentId, UUID requesterId) {
        Document document = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        User requester = requesterId != null ? userRepository.findById(requesterId).orElse(null) : null;
        auditLogService.logEvent(document.getTenant(), requester, "DOCUMENT_VIEWED", "DOCUMENT", document.getId(), "{\"action\":\"VIEW_EXTRACTED_TEXT\"}", "SUCCESS");

        return new DocumentTextResponse(
                document.getId(),
                document.getTenant().getId(),
                document.getTitle(),
                document.getStatus().name(),
                document.getExtractedText() != null ? document.getExtractedText() : ""
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getDocumentChunks(UUID tenantId, UUID documentId, UUID requesterId) {
        Document document = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        List<DocumentChunk> chunks = documentChunkRepository.findByTenantIdAndDocumentIdOrderByChunkIndexAsc(tenantId, documentId);

        return chunks.stream()
                .map(this::mapToDocumentChunkResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "document-metadata", key = "'tenant:' + #tenantId + ':document:' + #documentId")
    public DocumentResponse updateDocument(UUID tenantId, UUID documentId, DocumentUpdateRequest request, UUID updaterId) {
        Document document = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (request.title() != null && !request.title().isBlank()) {
            document.setTitle(request.title());
        }
        if (request.description() != null) {
            document.setDescription(request.description());
        }
        if (request.category() != null && !request.category().isBlank()) {
            try {
                document.setCategory(DocumentCategory.valueOf(request.category().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid category
            }
        }
        if (request.tags() != null) {
            document.setTags(request.tags());
        }
        if (request.securityClassification() != null && !request.securityClassification().isBlank()) {
            try {
                document.setSecurityClassification(SecurityClassification.valueOf(request.securityClassification().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid
            }
        }

        document.setVersion(document.getVersion() + 1);
        Document savedDoc = documentRepository.save(document);

        User updater = updaterId != null ? userRepository.findById(updaterId).orElse(null) : null;
        auditLogService.logEvent(document.getTenant(), updater, "DOCUMENT_UPDATED", "DOCUMENT", document.getId(), "{}", "SUCCESS");

        return mapToDocumentResponse(savedDoc);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "document-metadata", key = "'tenant:' + #tenantId + ':document:' + #documentId")
    public void deleteDocument(UUID tenantId, UUID documentId, UUID deleterId) {
        Document document = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        // Delete associated chunks first
        documentChunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
        documentStorageService.delete(document.getSourceUri());
        documentRepository.delete(document);

        User deleter = deleterId != null ? userRepository.findById(deleterId).orElse(null) : null;
        auditLogService.logEvent(document.getTenant(), deleter, "DOCUMENT_DELETED", "DOCUMENT", documentId, "{}", "SUCCESS");

        log.info("Successfully deleted document ID: {}", documentId);
    }

    private void validateFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File must have a name");
        }

        String lowerFilename = filename.toLowerCase();
        String contentType = file.getContentType();

        boolean isPdf = lowerFilename.endsWith(".pdf") && "application/pdf".equals(contentType);
        boolean isDocx = lowerFilename.endsWith(".docx") && "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
        boolean isTxt = lowerFilename.endsWith(".txt") && "text/plain".equals(contentType);

        if (!isPdf && !isDocx && !isTxt) {
            throw new IllegalArgumentException("Unsupported file type or extension mismatch. Allowed: PDF, DOCX, TXT");
        }

        // Magic bytes check
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header, 0, 4);
            if (read >= 4) {
                if (isPdf) {
                    // %PDF
                    if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                        throw new IllegalArgumentException("Invalid PDF file signature");
                    }
                } else if (isDocx) {
                    // PK
                    if (header[0] != 0x50 || header[1] != 0x4B) {
                        throw new IllegalArgumentException("Invalid DOCX file signature");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read file for validation", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown";
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        sanitized = sanitized.replace("\0", "");
        return sanitized;
    }

    private DocumentResponse mapToDocumentResponse(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getTenant().getId(),
                doc.getTitle(),
                doc.getOriginalFileName(),
                doc.getDescription(),
                doc.getCategory().name(),
                doc.getSourceType(),
                doc.getMimeType(),
                doc.getFileSizeBytes(),
                doc.getChecksum(),
                doc.getSecurityClassification().name(),
                doc.getAllowedRoles(),
                doc.getAllowedDepartments(),
                doc.getTags(),
                doc.getStatus().name(),
                doc.getVersion(),
                doc.getUploader() != null ? doc.getUploader().getFullName() : "System",
                doc.getCreatedAt()
        );
    }

    private DocumentSummaryResponse mapToDocumentSummaryResponse(Document doc) {
        return new DocumentSummaryResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory().name(),
                doc.getMimeType(),
                doc.getFileSizeBytes(),
                doc.getStatus().name(),
                doc.getUploader() != null ? doc.getUploader().getFullName() : "System",
                doc.getCreatedAt()
        );
    }

    private DocumentChunkResponse mapToDocumentChunkResponse(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.getId(),
                chunk.getTenant().getId(),
                chunk.getDocument().getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getCharacterCount(),
                chunk.getTokenEstimate(),
                chunk.getPageNumber(),
                chunk.getMetadata(),
                chunk.getSecurityClassification().name(),
                chunk.getCreatedAt()
        );
    }
}
