package com.enterprise.assistant.service.document.chunker;

import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.document.DocumentCategory;
import com.enterprise.assistant.domain.document.DocumentChunk;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.SecurityClassification;
import com.enterprise.assistant.service.document.chunker.impl.DefaultTokenEstimator;
import com.enterprise.assistant.service.document.chunker.impl.RecursiveDocumentChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveDocumentChunkerTest {

    private RecursiveDocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new RecursiveDocumentChunker(new DefaultTokenEstimator());
    }

    @Test
    void chunk_EmptyText_ReturnsEmptyList() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .extractedText("")
                .build();

        List<DocumentChunk> chunks = chunker.chunk(doc, new ChunkingOptions(100, 10));
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunk_ShortText_ReturnsSingleChunk() {
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("Acme").build();
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .title("Short Document")
                .originalFileName("short.txt")
                .category(DocumentCategory.GENERAL)
                .securityClassification(SecurityClassification.INTERNAL)
                .extractedText("This is a short document paragraph that easily fits in one chunk.")
                .version(1)
                .build();

        List<DocumentChunk> chunks = chunker.chunk(doc, new ChunkingOptions(100, 10));
        assertEquals(1, chunks.size());

        DocumentChunk chunk = chunks.get(0);
        assertEquals(0, chunk.getChunkIndex());
        assertTrue(chunk.getContent().contains("short document"));
        assertNotNull(chunk.getMetadata());
        assertTrue(chunk.getMetadata().contains("short.txt"));
    }

    @Test
    void chunk_MultiParagraphText_SplitsAndAppliesOverlap() {
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("Acme").build();

        StringBuilder sb = new StringBuilder();
        sb.append("Paragraph 1. ").append("A ".repeat(150)).append("\n\n");
        sb.append("Paragraph 2. ").append("B ".repeat(150)).append("\n\n");
        sb.append("Paragraph 3. ").append("C ".repeat(150));

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .title("Multi Paragraph Document")
                .originalFileName("multi.txt")
                .category(DocumentCategory.GENERAL)
                .securityClassification(SecurityClassification.INTERNAL)
                .extractedText(sb.toString())
                .version(1)
                .build();

        // Target 50 tokens (~200 chars) per chunk with 10 tokens (~40 chars) overlap
        List<DocumentChunk> chunks = chunker.chunk(doc, new ChunkingOptions(50, 10));
        assertTrue(chunks.size() > 1);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex());
            assertNotNull(chunks.get(i).getContent());
            assertTrue(chunks.get(i).getTokenEstimate() > 0);
        }
    }
}
