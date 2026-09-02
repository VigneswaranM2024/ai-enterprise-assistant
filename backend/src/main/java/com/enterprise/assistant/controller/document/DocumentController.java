package com.enterprise.assistant.controller.document;

import com.enterprise.assistant.dto.request.DocumentUpdateRequest;
import com.enterprise.assistant.dto.response.*;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Document Management REST Controller exposing upload, download, text extraction, chunking, and delete endpoints.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "Endpoints for uploading, downloading, searching, and chunking knowledge documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    @Operation(summary = "Upload document", description = "Uploads a raw file for smart parsing, text extraction, and semantic chunking")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false, defaultValue = "GENERAL") String category,
            @RequestParam(value = "securityClassification", required = false, defaultValue = "INTERNAL") String securityClassification,
            @RequestParam(value = "allowedRoles", required = false) String[] allowedRoles,
            @RequestParam(value = "tags", required = false) String[] tags
    ) {
        DocumentResponse response = documentService.uploadDocument(
                currentUser.getTenantId(),
                currentUser.getId(),
                file,
                title,
                category,
                securityClassification,
                allowedRoles,
                tags
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @Operation(summary = "Search documents", description = "Retrieves a paginated list of documents with title and category filtering")
    public ResponseEntity<PageResponse<DocumentSummaryResponse>> getDocuments(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Search query matching title") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by document category") @RequestParam(required = false) String category,
            @Parameter(description = "Page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<DocumentSummaryResponse> response = documentService.getDocuments(currentUser.getTenantId(), query, category, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @Operation(summary = "Get document details by ID", description = "Fetches document metadata, file size, checksum, and status")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        DocumentResponse response = documentService.getDocumentById(currentUser.getTenantId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_DOWNLOAD')")
    @Operation(summary = "Download raw file", description = "Streams raw document file binary for download")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        DocumentResponse docMetadata = documentService.getDocumentById(currentUser.getTenantId(), id);
        Resource fileResource = documentService.downloadDocument(currentUser.getTenantId(), id, currentUser.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(docMetadata.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docMetadata.originalFileName() + "\"")
                .body(fileResource);
    }

    @GetMapping("/{id}/text")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @Operation(summary = "Get extracted document text", description = "Retrieves the normalized extracted text content for a document")
    public ResponseEntity<DocumentTextResponse> getDocumentText(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        DocumentTextResponse response = documentService.getDocumentText(currentUser.getTenantId(), id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/chunks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get document chunks (Admin)", description = "Debug endpoint for administrators to inspect generated document chunks")
    public ResponseEntity<List<DocumentChunkResponse>> getDocumentChunks(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        List<DocumentChunkResponse> response = documentService.getDocumentChunks(currentUser.getTenantId(), id, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_UPDATE')")
    @Operation(summary = "Update document metadata", description = "Update the title, category, tags, and classification of a document")
    public ResponseEntity<DocumentResponse> updateDocument(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody DocumentUpdateRequest request
    ) {
        DocumentResponse response = documentService.updateDocument(currentUser.getTenantId(), id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    @Operation(summary = "Delete document", description = "Admin endpoint to purge raw file, metadata, and generated chunks")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        documentService.deleteDocument(currentUser.getTenantId(), id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
