package com.enterprise.assistant.service.search;

import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
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

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    private SemanticSearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        searchService = new SemanticSearchServiceImpl(embeddingService, documentChunkRepository);
    }

    @Test
    void search_NullQuery_ThrowsIllegalArgumentException() {
        UUID tenantId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> searchService.search(tenantId, "", 5));
    }

    @Test
    void search_ValidQuery_ReturnsTopKResults() {
        UUID tenantId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        List<Float> mockQueryVector = new ArrayList<>(Collections.nCopies(768, 0.05f));
        when(embeddingService.generateEmbedding(eq("How to request leave?"), eq(GeminiTaskType.RETRIEVAL_QUERY)))
                .thenReturn(mockQueryVector);

        Object[] rawRow = new Object[]{chunkId, docId, "Employees apply for leave via HR portal.", 0.89d, "{\"title\":\"Leave Policy\"}"};
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(rawRow);
        when(documentChunkRepository.findSimilarChunksNative(eq(tenantId), anyString(), eq(5)))
                .thenReturn(mockRows);

        SemanticSearchResponse response = searchService.search(tenantId, "How to request leave?", 5);

        assertNotNull(response);
        assertEquals("How to request leave?", response.query());
        assertEquals(1, response.totalResults());
        assertEquals(chunkId, response.results().get(0).chunkId());
        assertEquals(0.89d, response.results().get(0).score());
        assertEquals("Employees apply for leave via HR portal.", response.results().get(0).content());

        verify(embeddingService).generateEmbedding("How to request leave?", GeminiTaskType.RETRIEVAL_QUERY);
        verify(documentChunkRepository).findSimilarChunksNative(eq(tenantId), anyString(), eq(5));
    }

    @Test
    void search_EmptyDatabaseResults_ReturnsZeroCountResponse() {
        UUID tenantId = UUID.randomUUID();
        List<Float> mockQueryVector = new ArrayList<>(Collections.nCopies(768, 0.05f));
        when(embeddingService.generateEmbedding(anyString(), any(GeminiTaskType.class))).thenReturn(mockQueryVector);

        when(documentChunkRepository.findSimilarChunksNative(eq(tenantId), anyString(), eq(5)))
                .thenReturn(Collections.emptyList());

        SemanticSearchResponse response = searchService.search(tenantId, "Unknown topic", 5);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertTrue(response.results().isEmpty());
    }
}
