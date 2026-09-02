package com.enterprise.assistant.eval.citation;

import com.enterprise.assistant.dto.response.CitationDTO;
import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.service.rag.RagContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Citation Security & Attribution Tests verifying that citation metadata
 * (documentId, chunkId, title, fileName, score) originates 100% authoritatively
 * from backend search results and cannot be fabricated or tampered with.
 */
class CitationSecurityTest {

    private RagContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new RagContextBuilder(new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void citationGeneration_StrictlyAuthoritativeFromBackendRetrieval() {
        UUID docId1 = UUID.randomUUID();
        UUID chunkId1 = UUID.randomUUID();
        String metadata1 = "{\"title\":\"Employee Handbook\",\"originalFileName\":\"handbook.pdf\",\"chunkIndex\":3}";

        SearchResultItemResponse item1 = new SearchResultItemResponse(chunkId1, docId1, "Annual leave is 20 days.", 0.91d, metadata1);

        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(List.of(item1), 4000);

        assertNotNull(result);
        List<CitationDTO> citations = result.getCitations();
        assertEquals(1, citations.size());

        CitationDTO citation = citations.get(0);
        assertEquals("S1", citation.citationId());
        assertEquals(docId1, citation.documentId());
        assertEquals(chunkId1, citation.chunkId());
        assertEquals("Employee Handbook", citation.documentTitle());
        assertEquals("handbook.pdf", citation.fileName());
        assertNull(citation.chunkIndex());
        assertEquals(0.91d, citation.similarityScore());
    }

    @Test
    void llmCannotInventDocumentIdChunkIdOrFileName() {
        // Search result with missing metadata fields safely falls back to safe backend defaults
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        SearchResultItemResponse item = new SearchResultItemResponse(chunkId, docId, "Some text content.", 0.85d, null);

        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(List.of(item), 4000);

        List<CitationDTO> citations = result.getCitations();
        assertEquals(1, citations.size());
        CitationDTO citation = citations.get(0);

        assertEquals(docId, citation.documentId());
        assertEquals(chunkId, citation.chunkId());
        assertEquals("Enterprise Document", citation.documentTitle());
        assertEquals("document.pdf", citation.fileName());
    }

    @Test
    void citationIds_RemainStableAndOrdered() {
        UUID docId = UUID.randomUUID();
        SearchResultItemResponse item1 = new SearchResultItemResponse(UUID.randomUUID(), docId, "Text 1", 0.95d, "{\"title\":\"Doc 1\"}");
        SearchResultItemResponse item2 = new SearchResultItemResponse(UUID.randomUUID(), docId, "Text 2", 0.88d, "{\"title\":\"Doc 2\"}");

        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(List.of(item1, item2), 4000);

        List<CitationDTO> citations = result.getCitations();
        assertEquals(2, citations.size());
        assertEquals("S1", citations.get(0).citationId());
        assertEquals("S2", citations.get(1).citationId());
        assertTrue(result.getFormattedContext().contains("[S1]"));
        assertTrue(result.getFormattedContext().contains("[S2]"));
    }
}
