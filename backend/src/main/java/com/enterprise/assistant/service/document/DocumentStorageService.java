package com.enterprise.assistant.service.document;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Service Contract for Document Storage Operations.
 */
public interface DocumentStorageService {

    String store(UUID tenantId, UUID documentId, MultipartFile file);

    Resource retrieve(String storageKey);

    void delete(String storageKey);
    
    boolean exists(String storageKey);
}
