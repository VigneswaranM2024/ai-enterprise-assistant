package com.enterprise.assistant.service.rag;

import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RagContextBuilderTest {

    private RagContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new RagContextBuilder(new ObjectMapper());
    }

    @Test
    void buildContext_NullOrEmptyItems_ReturnsEmptyContext() {
        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(null, 6000);
        assertNotNull(result);
        assertEquals("", result.getFormattedContext());
        assertTrue(result.getCitations().isEmpty());
    }

    @Test
    void buildContext_ValidItems_BuildsFormattedContextAndCitations() {
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String metadataJson = "{\"title\":\"Employee Leave Policy 2026\",\"originalFileName\":\"leave.pdf\"}";

        SearchResultItemResponse item = new SearchResultItemResponse(
                chunkId,
                docId,
                "Employees are entitled to 20 days of paid leave per year.",
                0.89d,
                metadataJson
        );

        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(List.of(item), 6000);

        assertNotNull(result);
        assertTrue(result.getFormattedContext().contains("[S1] Document: Employee Leave Policy 2026 | File: leave.pdf"));
        assertTrue(result.getFormattedContext().contains("Employees are entitled to 20 days of paid leave per year."));

        assertEquals(1, result.getCitations().size());
        assertEquals("S1", result.getCitations().get(0).citationId());
        assertEquals(docId, result.getCitations().get(0).documentId());
        assertEquals("Employee Leave Policy 2026", result.getCitations().get(0).documentTitle());
        assertEquals("leave.pdf", result.getCitations().get(0).fileName());
    }

    @Test
    void buildContext_ContextCharacterLimit_TruncatesOverflowChunks() {
        UUID docId = UUID.randomUUID();
        String metadataJson = "{\"title\":\"Doc\",\"originalFileName\":\"file.pdf\"}";

        SearchResultItemResponse item1 = new SearchResultItemResponse(UUID.randomUUID(), docId, "Chunk 1 Content " + "A".repeat(200), 0.90d, metadataJson);
        SearchResultItemResponse item2 = new SearchResultItemResponse(UUID.randomUUID(), docId, "Chunk 2 Content " + "B".repeat(200), 0.85d, metadataJson);

        // Limit context to ~50 tokens (~200 chars) so item2 gets truncated
        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(List.of(item1, item2), 50);

        assertNotNull(result);
        assertEquals(1, result.getCitations().size());
        assertEquals("S1", result.getCitations().get(0).citationId());
    }
}
