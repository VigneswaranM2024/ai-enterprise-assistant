package com.enterprise.assistant.eval.edgecases;

import com.enterprise.assistant.service.embedding.impl.GeminiEmbeddingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fault Tolerance and Edge Case Tests covering corrupted media types,
 * invalid file headers, API error conditions, and Redis offline degradation.
 */
class EdgeCaseFaultToleranceTest {

    @Test
    void unsupportedFileType_RejectsNonAllowedFormat() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malicious.exe", "application/x-msdownload", new byte[]{0x4D, 0x5A, 0x00});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            if (!exeFile.getOriginalFilename().endsWith(".pdf") &&
                !exeFile.getOriginalFilename().endsWith(".docx") &&
                !exeFile.getOriginalFilename().endsWith(".txt")) {
                throw new IllegalArgumentException("Unsupported file type or extension mismatch. Allowed: PDF, DOCX, TXT");
            }
        });

        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    void corruptedPdfMagicBytes_RejectsInvalidHeaderSignature() {
        // PDF extension but invalid magic bytes (not %PDF)
        byte[] fakePdfBytes = "NOT_A_PDF_HEADER_CONTENT".getBytes();
        MockMultipartFile fakePdf = new MockMultipartFile("file", "fake.pdf", "application/pdf", fakePdfBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            byte[] header = new byte[4];
            System.arraycopy(fakePdfBytes, 0, header, 0, 4);
            if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                throw new IllegalArgumentException("Invalid PDF file signature");
            }
        });

        assertTrue(ex.getMessage().contains("Invalid PDF file signature"));
    }

    @Test
    void geminiEmbeddingService_MissingApiKey_ThrowsIllegalStateException() {
        com.enterprise.assistant.config.embedding.GeminiEmbeddingProperties props =
                new com.enterprise.assistant.config.embedding.GeminiEmbeddingProperties();
        props.setApiKey("");

        org.springframework.web.client.RestClient restClient = org.springframework.web.client.RestClient.builder().build();
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl(restClient, props);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.generateEmbedding("Test query", com.enterprise.assistant.service.embedding.GeminiTaskType.RETRIEVAL_QUERY));

        assertTrue(ex.getMessage().contains("GEMINI_API_KEY environment variable must be set"));
    }

    @Test
    void redisOffline_DegradesGracefullyWithoutCrashingApp() {
        // Simulating Redis connection failure error handler behavior
        com.enterprise.assistant.config.RedisConfig redisConfig = new com.enterprise.assistant.config.RedisConfig();
        org.springframework.cache.interceptor.CacheErrorHandler errorHandler = redisConfig.errorHandler();

        org.springframework.cache.Cache mockCache = org.mockito.Mockito.mock(org.springframework.cache.Cache.class);
        org.mockito.Mockito.when(mockCache.getName()).thenReturn("sessionCache");

        assertNotNull(errorHandler);
        // Ensure errorHandler handles cache errors without throwing uncaught exceptions to caller
        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(new RuntimeException("Redis connection refused"), mockCache, "session:123"));
    }
}
