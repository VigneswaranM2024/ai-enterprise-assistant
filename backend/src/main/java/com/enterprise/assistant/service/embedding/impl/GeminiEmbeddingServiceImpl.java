package com.enterprise.assistant.service.embedding.impl;

import com.enterprise.assistant.config.embedding.GeminiEmbeddingProperties;
import com.enterprise.assistant.dto.ai.gemini.GeminiBatchEmbedRequest;
import com.enterprise.assistant.dto.ai.gemini.GeminiBatchEmbedResponse;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of EmbeddingService utilizing Google Gemini gemini-embedding-2 API.
 * Supports batch processing and MRL output dimensionality configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiEmbeddingServiceImpl implements EmbeddingService {

    private final RestClient geminiEmbeddingRestClient;
    private final GeminiEmbeddingProperties embeddingProperties;

    @Override
    public List<Float> generateEmbedding(String text, GeminiTaskType taskType) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text content for embedding generation must not be empty");
        }
        List<List<Float>> results = generateEmbeddings(List.of(text), taskType);
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("Failed to generate embedding vector for input text");
        }
        return results.get(0);
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts, GeminiTaskType taskType) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        validateApiKey();

        int batchSize = Math.max(1, embeddingProperties.getBatchSize());
        List<List<Float>> allEmbeddings = new ArrayList<>(texts.size());

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batchTexts = texts.subList(i, end);

            List<List<Float>> batchResults = processBatch(batchTexts, taskType);
            allEmbeddings.addAll(batchResults);
        }

        return allEmbeddings;
    }

    private List<List<Float>> processBatch(List<String> texts, GeminiTaskType taskType) {
        String modelName = embeddingProperties.getModel();
        int targetDim = embeddingProperties.getDimension();
        String taskTypeStr = taskType != null ? taskType.name() : GeminiTaskType.RETRIEVAL_DOCUMENT.name();

        List<GeminiBatchEmbedRequest.EmbedContentRequest> requests = texts.stream()
                .map(text -> new GeminiBatchEmbedRequest.EmbedContentRequest(
                        modelName,
                        new GeminiBatchEmbedRequest.Content(List.of(new GeminiBatchEmbedRequest.Part(text))),
                        taskTypeStr,
                        targetDim
                ))
                .collect(Collectors.toList());

        GeminiBatchEmbedRequest payload = new GeminiBatchEmbedRequest(requests);

        String uri = String.format("/%s:batchEmbedContents?key=%s", modelName, embeddingProperties.getApiKey());
        log.debug("Dispatching batch embedding request for {} items (Model: {}, TaskType: {}, Dimension: {})", texts.size(), modelName, taskTypeStr, targetDim);

        try {
            ResponseEntity<GeminiBatchEmbedResponse> responseEntity = geminiEmbeddingRestClient.post()
                    .uri(uri)
                    .body(payload)
                    .retrieve()
                    .toEntity(GeminiBatchEmbedResponse.class);

            GeminiBatchEmbedResponse responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.embeddings() == null || responseBody.embeddings().isEmpty()) {
                log.warn("Gemini Embedding API returned empty embeddings array.");
                throw new IllegalStateException("Gemini Embedding API returned empty response.");
            }

            List<List<Float>> resultVectors = new ArrayList<>();
            for (GeminiBatchEmbedResponse.EmbeddingData data : responseBody.embeddings()) {
                if (data.values() == null || data.values().isEmpty()) {
                    throw new IllegalStateException("Received null or empty vector values from Gemini Embedding API.");
                }
                resultVectors.add(data.values());
            }

            return resultVectors;
        } catch (RestClientResponseException ex) {
            log.error("Gemini Embedding API HTTP Error - Status: {}, Message: {}", ex.getStatusCode(), ex.getStatusText());
            throw new RuntimeException("Gemini Embedding API call failed with status " + ex.getStatusCode() + ": " + ex.getStatusText(), ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                throw ex;
            }
            log.error("Gemini Embedding API invocation failed: {}", ex.getMessage());
            throw new RuntimeException("Gemini Embedding API invocation failed: " + ex.getMessage(), ex);
        }
    }

    private void validateApiKey() {
        if (embeddingProperties.getApiKey() == null || embeddingProperties.getApiKey().isBlank()) {
            log.error("Gemini Embedding invocation aborted: GEMINI_API_KEY environment variable is not configured.");
            throw new IllegalStateException("GEMINI_API_KEY environment variable must be set and non-empty for Gemini embedding integration.");
        }
    }
}
