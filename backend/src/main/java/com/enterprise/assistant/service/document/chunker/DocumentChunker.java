package com.enterprise.assistant.service.document.chunker;

import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.document.DocumentChunk;

import java.util.List;

/**
 * Interface contract for chunking document text into semantic chunks for vector storage and RAG.
 */
public interface DocumentChunker {

    /**
     * Chunks a document into semantic DocumentChunk records.
     *
     * @param document Source document domain entity
     * @param options Chunking parameters (target tokens, overlap tokens)
     * @return List of DocumentChunk entities ready for persistence
     */
    List<DocumentChunk> chunk(Document document, ChunkingOptions options);
}
