package com.enterprise.assistant.service.embedding;

import com.enterprise.assistant.config.embedding.GeminiEmbeddingProperties;
import com.enterprise.assistant.dto.ai.gemini.GeminiBatchEmbedRequest;
import com.enterprise.assistant.dto.ai.gemini.GeminiBatchEmbedResponse;
import com.enterprise.assistant.service.embedding.impl.GeminiEmbeddingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiEmbeddingServiceImplTest {

    @Mock
    private RestClient geminiEmbeddingRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiEmbeddingProperties embeddingProperties;
    private GeminiEmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        embeddingProperties = new GeminiEmbeddingProperties();
        embeddingProperties.setProvider("gemini");
        embeddingProperties.setApiKey("AIzaSyTestApiKeyMock123456789");
        embeddingProperties.setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        embeddingProperties.setModel("models/gemini-embedding-2");
        embeddingProperties.setDimension(768);
        embeddingProperties.setBatchSize(50);

        embeddingService = new GeminiEmbeddingServiceImpl(geminiEmbeddingRestClient, embeddingProperties);
    }

    @Test
    void generateEmbedding_MissingApiKey_ThrowsIllegalStateException() {
        embeddingProperties.setApiKey("");
        assertThrows(IllegalStateException.class, () -> embeddingService.generateEmbedding("Test", GeminiTaskType.RETRIEVAL_QUERY));
    }

    @Test
    void generateEmbedding_EmptyText_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> embeddingService.generateEmbedding("", GeminiTaskType.RETRIEVAL_QUERY));
    }

    @Test
    void generateEmbedding_Success_Returns768DimensionalVector() {
        List<Float> mockVector = new ArrayList<>(Collections.nCopies(768, 0.123f));
        GeminiBatchEmbedResponse mockResponse = new GeminiBatchEmbedResponse(List.of(new GeminiBatchEmbedResponse.EmbeddingData(mockVector)));

        when(geminiEmbeddingRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GeminiBatchEmbedRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(GeminiBatchEmbedResponse.class)).thenReturn(ResponseEntity.ok(mockResponse));

        List<Float> result = embeddingService.generateEmbedding("Sample paragraph text", GeminiTaskType.RETRIEVAL_DOCUMENT);

        assertNotNull(result);
        assertEquals(768, result.size());
        assertEquals(0.123f, result.get(0));
        verify(geminiEmbeddingRestClient).post();
    }

    @Test
    void generateEmbeddings_BatchProcessing_SplitsByBatchSize() {
        embeddingProperties.setBatchSize(2);

        List<Float> vec1 = new ArrayList<>(Collections.nCopies(768, 0.1f));
        List<Float> vec2 = new ArrayList<>(Collections.nCopies(768, 0.2f));
        GeminiBatchEmbedResponse batch1Response = new GeminiBatchEmbedResponse(List.of(
                new GeminiBatchEmbedResponse.EmbeddingData(vec1),
                new GeminiBatchEmbedResponse.EmbeddingData(vec2)
        ));
        GeminiBatchEmbedResponse batch2Response = new GeminiBatchEmbedResponse(List.of(
                new GeminiBatchEmbedResponse.EmbeddingData(vec1)
        ));

        when(geminiEmbeddingRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GeminiBatchEmbedRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(GeminiBatchEmbedResponse.class))
                .thenReturn(ResponseEntity.ok(batch1Response))
                .thenReturn(ResponseEntity.ok(batch2Response));

        List<String> inputs = List.of("Chunk 1", "Chunk 2", "Chunk 3");
        List<List<Float>> results = embeddingService.generateEmbeddings(inputs, GeminiTaskType.RETRIEVAL_DOCUMENT);

        assertNotNull(results);
        assertEquals(3, results.size());
        verify(geminiEmbeddingRestClient, times(2)).post();
    }

    @Test
    void generateEmbedding_ApiError_ThrowsRuntimeExceptionWithoutExposingKey() {
        when(geminiEmbeddingRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GeminiBatchEmbedRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientResponseException("Forbidden", HttpStatus.FORBIDDEN.value(), "Forbidden", null, null, null));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> embeddingService.generateEmbedding("Test query", GeminiTaskType.RETRIEVAL_QUERY));
        assertTrue(exception.getMessage().contains("403"));
        assertFalse(exception.getMessage().contains("AIzaSyTestApiKeyMock123456789"));
    }
}
