package com.enterprise.assistant.service.embedding;

import java.util.List;

/**
 * Service Abstraction for generating vector embeddings.
 */
public interface EmbeddingService {

    /**
     * Generates a vector embedding for a single text input.
     *
     * @param text Input string content
     * @param taskType Intended task type (RETRIEVAL_DOCUMENT vs RETRIEVAL_QUERY)
     * @return List of float values representing vector components
     */
    List<Float> generateEmbedding(String text, GeminiTaskType taskType);

    /**
     * Generates vector embeddings for a list of text inputs in batches.
     *
     * @param texts List of input string contents
     * @param taskType Intended task type (RETRIEVAL_DOCUMENT vs RETRIEVAL_QUERY)
     * @return List of float vector representations
     */
    List<List<Float>> generateEmbeddings(List<String> texts, GeminiTaskType taskType);
}
