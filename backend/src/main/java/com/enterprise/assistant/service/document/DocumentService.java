package com.enterprise.assistant.service.document;

import com.enterprise.assistant.dto.request.DocumentUpdateRequest;
import com.enterprise.assistant.dto.response.DocumentChunkResponse;
import com.enterprise.assistant.dto.response.DocumentResponse;
import com.enterprise.assistant.dto.response.DocumentSummaryResponse;
import com.enterprise.assistant.dto.response.DocumentTextResponse;
import com.enterprise.assistant.dto.response.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service Contract for Document Management Operations.
 */
public interface DocumentService {

    DocumentResponse uploadDocument(
        UUID tenantId,
        UUID uploaderId,
        MultipartFile file,
        String title,
        String category,
        String securityClassification,
        String[] allowedRoles,
        String[] tags
    );

    PageResponse<DocumentSummaryResponse> getDocuments(UUID tenantId, String query, String category, Pageable pageable);

    DocumentResponse getDocumentById(UUID tenantId, UUID documentId);

    Resource downloadDocument(UUID tenantId, UUID documentId, UUID downloaderId);

    DocumentTextResponse getDocumentText(UUID tenantId, UUID documentId, UUID requesterId);

    List<DocumentChunkResponse> getDocumentChunks(UUID tenantId, UUID documentId, UUID requesterId);
    
    DocumentResponse updateDocument(UUID tenantId, UUID documentId, DocumentUpdateRequest request, UUID updaterId);

    void deleteDocument(UUID tenantId, UUID documentId, UUID deleterId);
}
