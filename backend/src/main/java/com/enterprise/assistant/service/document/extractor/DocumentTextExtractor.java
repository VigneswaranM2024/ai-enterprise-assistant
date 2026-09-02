package com.enterprise.assistant.service.document.extractor;

import org.springframework.core.io.Resource;

/**
 * Clean abstraction interface for extracting text content from document resources.
 */
public interface DocumentTextExtractor {

    /**
     * Checks if this extractor supports the given MIME type.
     */
    boolean supports(String mimeType);

    /**
     * Extracts text from the provided document resource.
     *
     * @param resource Document file resource
     * @param mimeType MIME type of the document
     * @return Extracted raw text
     */
    String extractText(Resource resource, String mimeType);
}
