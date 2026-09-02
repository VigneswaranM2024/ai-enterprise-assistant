package com.enterprise.assistant.service.document.processor;

/**
 * Text normalizer interface for cleaning extracted document text prior to downstream RAG operations.
 */
public interface TextNormalizer {

    /**
     * Normalizes raw extracted text by removing control characters, trimming lines,
     * and consolidating whitespace while preserving paragraph and sentence structure.
     *
     * @param rawText Raw extracted text
     * @return Cleaned and normalized text
     */
    String normalize(String rawText);
}
