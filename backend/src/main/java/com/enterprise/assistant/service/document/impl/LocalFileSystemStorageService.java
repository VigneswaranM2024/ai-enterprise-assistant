package com.enterprise.assistant.service.document.impl;

import com.enterprise.assistant.exception.ResourceNotFoundException;
import com.enterprise.assistant.service.document.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementation of DocumentStorageService managing local file persistence.
 */
@Service
@Slf4j
public class LocalFileSystemStorageService implements DocumentStorageService {

    private Path storageBasePath;
    
    @Value("${document.storage.path:storage/documents/}")
    private String storagePathStr;

    @PostConstruct
    public void init() {
        this.storageBasePath = Paths.get(storagePathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageBasePath);
        } catch (Exception ex) {
            log.error("Could not create local storage base directory", ex);
        }
    }

    @Override
    public String store(UUID tenantId, UUID documentId, MultipartFile file) {
        try {
            Path tenantPath = this.storageBasePath.resolve(tenantId.toString()).normalize();
            if (!tenantPath.startsWith(this.storageBasePath)) {
                throw new SecurityException("Cannot store file outside of intended directory.");
            }
            Files.createDirectories(tenantPath);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                // Remove null characters just in case
                extension = extension.replace("\0", "");
            }

            String targetFileName = documentId.toString() + extension;
            Path targetLocation = tenantPath.resolve(targetFileName).normalize();
            
            if (!targetLocation.startsWith(tenantPath)) {
                throw new SecurityException("Cannot store file outside of tenant directory.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            log.error("Failed to store file for tenant: {}, document: {}", tenantId, documentId, ex);
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    @Override
    public Resource retrieve(String storageKey) {
        try {
            Path filePath = Paths.get(storageKey).normalize();
            if (!filePath.startsWith(this.storageBasePath)) {
                throw new SecurityException("Cannot retrieve file outside of intended directory.");
            }
            
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable at path");
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File path is malformed");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path filePath = Paths.get(storageKey).normalize();
            if (!filePath.startsWith(this.storageBasePath)) {
                throw new SecurityException("Cannot delete file outside of intended directory.");
            }
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Could not delete file at path: {}", storageKey, ex);
        }
    }
    
    @Override
    public boolean exists(String storageKey) {
        Path filePath = Paths.get(storageKey).normalize();
        if (!filePath.startsWith(this.storageBasePath)) {
            return false;
        }
        return Files.exists(filePath);
    }
}
