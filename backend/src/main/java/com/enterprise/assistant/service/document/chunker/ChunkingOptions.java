package com.enterprise.assistant.service.document.chunker;

/**
 * Options for document chunking behavior.
 */
public record ChunkingOptions(
    int targetTokens,
    int overlapTokens
) {
    public ChunkingOptions {
        if (targetTokens <= 0) {
            throw new IllegalArgumentException("targetTokens must be greater than 0");
        }
        if (overlapTokens < 0 || overlapTokens >= targetTokens) {
            throw new IllegalArgumentException("overlapTokens must be non-negative and strictly less than targetTokens");
        }
    }
}
