package com.enterprise.assistant.service.document.extractor.impl;

import com.enterprise.assistant.service.document.extractor.DocumentTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Implementation of DocumentTextExtractor backed by Apache Tika engine.
 * Supports PDF, DOCX, and TXT format parsing.
 */
@Component
@Slf4j
public class TikaDocumentTextExtractor implements DocumentTextExtractor {

    private final Tika tika = new Tika();

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown"
    );

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    @Override
    public String extractText(Resource resource, String mimeType) {
        if (resource == null || !resource.exists()) {
            throw new IllegalArgumentException("Document resource does not exist or is missing");
        }

        log.debug("Extracting text from resource: {} (MIME: {})", resource.getFilename(), mimeType);

        try (InputStream is = resource.getInputStream()) {
            String extracted = tika.parseToString(is);
            if (extracted == null) {
                return "";
            }
            return extracted;
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from document: {}", resource.getFilename(), e);
            throw new RuntimeException("Text extraction failed for file: " + resource.getFilename(), e);
        }
    }
}
