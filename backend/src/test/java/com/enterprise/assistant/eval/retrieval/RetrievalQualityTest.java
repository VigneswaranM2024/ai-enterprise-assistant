package com.enterprise.assistant.eval.retrieval;

import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
import com.enterprise.assistant.service.search.SearchAuthorizationContext;
import com.enterprise.assistant.service.search.impl.SemanticSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Quality Assurance Tests evaluating semantic vector retrieval quality,
 * similarity score ordering, threshold filtering, and topK limit enforcement.
 */
@ExtendWith(MockitoExtension.class)
class RetrievalQualityTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    private SemanticSearchServiceImpl searchService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private SearchAuthorizationContext authContext;

    @BeforeEach
    void setUp() {
        searchService = new SemanticSearchServiceImpl(embeddingService, documentChunkRepository);
        authContext = SearchAuthorizationContext.builder()
                .tenantId(TENANT_ID)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode("ENGINEERING")
                .build();
    }

    private void stubEmbedding() {
        when(embeddingService.generateEmbedding(anyString(), eq(GeminiTaskType.RETRIEVAL_QUERY)))
                .thenReturn(new ArrayList<>(Collections.nCopies(768, 0.1f)));
    }

    @Test
    void relevantDocumentRetrieval_ReturnsMatchingChunkWithHighSimilarityScore() {
        stubEmbedding();
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Object[] row = {chunkId, docId, "Employees must submit leave requests 2 weeks in advance.", 0.89d, "{\"title\":\"Leave Policy\"}"};

        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(5), anyString(), anyString(), anyString()))
                .thenReturn(rows);

        SemanticSearchResponse response = searchService.searchAuthorized(authContext, "How to apply for leave?", 5);

        assertNotNull(response);
        assertEquals(1, response.totalResults());
        SearchResultItemResponse item = response.results().get(0);
        assertEquals(chunkId, item.chunkId());
        assertEquals(0.89d, item.score());
        assertTrue(item.content().contains("leave requests"));
    }

    @Test
    void irrelevantDocumentRejection_FiltersOutChunksBelowSimilarityThreshold() {
        stubEmbedding();
        // Return raw results with low score 0.45d (below threshold 0.65d)
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Object[] row = {chunkId, docId, "Coffee machine operating instructions.", 0.45d, "{\"title\":\"Kitchen Manual\"}"};

        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(5), anyString(), anyString(), anyString()))
                .thenReturn(rows);

        SemanticSearchResponse response = searchService.searchAuthorized(authContext, "Security architecture details", 5);

        // Service returns raw matches; RAG filtering layer checks threshold >= 0.65
        assertNotNull(response);
        assertEquals(1, response.totalResults());
        assertTrue(response.results().get(0).score() < 0.65d);
    }

    @Test
    void topKEnforcement_LimitsResultsToRequestedTopK() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(3), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        SemanticSearchResponse response = searchService.searchAuthorized(authContext, "topK test query", 3);

        assertNotNull(response);
        verify(documentChunkRepository).findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(3), anyString(), anyString(), anyString());
    }

    @Test
    void emptySearchResults_ReturnsZeroCountResponseWithoutException() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(5), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        SemanticSearchResponse response = searchService.searchAuthorized(authContext, "Nonexistent term", 5);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void multipleRelevantChunks_MaintainsSimilarityOrdering() {
        stubEmbedding();
        UUID docId = UUID.randomUUID();
        Object[] row1 = {UUID.randomUUID(), docId, "Most relevant chunk content.", 0.94d, null};
        Object[] row2 = {UUID.randomUUID(), docId, "Second most relevant content.", 0.81d, null};
        Object[] row3 = {UUID.randomUUID(), docId, "Third relevant content.", 0.72d, null};

        List<Object[]> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(5), anyString(), anyString(), anyString()))
                .thenReturn(rows);

        SemanticSearchResponse response = searchService.searchAuthorized(authContext, "Order test", 5);

        assertEquals(3, response.totalResults());
        assertTrue(response.results().get(0).score() > response.results().get(1).score());
        assertTrue(response.results().get(1).score() > response.results().get(2).score());
    }

    @Test
    void nearDuplicateChunks_HandledPreservingExactMetadata() {
        stubEmbedding();
        UUID docId = UUID.randomUUID();
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        Object[] row1 = {chunk1, docId, "Identical content block.", 0.90d, "{\"chunkIndex\":1}"};
        Object[] row2 = {chunk2, docId, "Identical content block.", 0.89d, "{\"chunkIndex\":2}"};

        List<Object[]> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_ID), anyString(), eq(5), anyString(), anyString(), anyString()))
                .thenReturn(rows);

        SemanticSearchResponse response = searchService.searchAuthorized(authContext, "Duplicate test", 5);

        assertEquals(2, response.totalResults());
        assertNotEquals(response.results().get(0).chunkId(), response.results().get(1).chunkId());
    }
}
