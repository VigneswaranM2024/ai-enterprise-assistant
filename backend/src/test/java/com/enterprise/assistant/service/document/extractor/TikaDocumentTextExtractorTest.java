package com.enterprise.assistant.service.document.extractor;

import com.enterprise.assistant.service.document.extractor.impl.TikaDocumentTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

class TikaDocumentTextExtractorTest {

    private TikaDocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TikaDocumentTextExtractor();
    }

    @Test
    void supports_ValidMimeTypes_ReturnsTrue() {
        assertTrue(extractor.supports("text/plain"));
        assertTrue(extractor.supports("application/pdf"));
        assertTrue(extractor.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void supports_UnsupportedMimeType_ReturnsFalse() {
        assertFalse(extractor.supports("image/png"));
        assertFalse(extractor.supports("application/zip"));
        assertFalse(extractor.supports(null));
    }

    @Test
    void extractText_PlainTextResource_SuccessfullyExtractsText() {
        String content = "Hello World! This is a test plain text document.";
        Resource resource = new ByteArrayResource(content.getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };

        String extracted = extractor.extractText(resource, "text/plain");
        assertNotNull(extracted);
        assertTrue(extracted.contains("Hello World!"));
    }

    @Test
    void extractText_MissingResource_ThrowsIllegalArgumentException() {
        Resource missingResource = new ByteArrayResource(new byte[0]) {
            @Override
            public boolean exists() {
                return false;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> extractor.extractText(missingResource, "text/plain"));
    }
}
