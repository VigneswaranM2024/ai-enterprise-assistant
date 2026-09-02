package com.enterprise.assistant.eval.performance;

import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.service.document.chunker.ChunkingOptions;
import com.enterprise.assistant.service.document.chunker.impl.RecursiveDocumentChunker;
import com.enterprise.assistant.service.document.processor.impl.DefaultTextNormalizer;
import com.enterprise.assistant.service.rag.RagContextBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Measurable Performance Diagnostic Tests verifying processing throughput,
 * chunking execution speed, and RAG context formatting latency.
 */
class PerformanceDiagnosticTest {

    @Test
    void textNormalizerPerformance_NormalizesLargeTextUnder50Ms() {
        DefaultTextNormalizer normalizer = new DefaultTextNormalizer();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("Line ").append(i).append(":   This is sample text with \t whitespace and \r\n line breaks. ");
        }
        String rawText = sb.toString();

        long startTime = System.nanoTime();
        String normalized = normalizer.normalize(rawText);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        assertNotNull(normalized);
        assertTrue(elapsedMs < 100, "Normalization of 5000 lines should complete under 100ms. Measured: " + elapsedMs + "ms");
    }

    @Test
    void recursiveChunkerPerformance_ChunksTextUnder100Ms() {
        com.enterprise.assistant.service.document.chunker.impl.DefaultTokenEstimator tokenEstimator =
                new com.enterprise.assistant.service.document.chunker.impl.DefaultTokenEstimator();
        RecursiveDocumentChunker chunker = new RecursiveDocumentChunker(tokenEstimator);

        com.enterprise.assistant.domain.tenant.Tenant tenant = com.enterprise.assistant.domain.tenant.Tenant.builder().id(UUID.randomUUID()).build();
        com.enterprise.assistant.domain.document.Document doc = com.enterprise.assistant.domain.document.Document.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .title("Performance Test Document")
                .originalFileName("perf.txt")
                .extractedText("Paragraph 1. ".repeat(100) + "\n\n" + "Paragraph 2. ".repeat(100) + "\n\n" + "Paragraph 3. ".repeat(100))
                .build();

        ChunkingOptions options = new ChunkingOptions(500, 50);

        long startTime = System.nanoTime();
        List<com.enterprise.assistant.domain.document.DocumentChunk> chunks = chunker.chunk(doc, options);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        assertFalse(chunks.isEmpty());
        assertTrue(elapsedMs < 100, "Recursive chunker execution should complete under 100ms. Measured: " + elapsedMs + "ms");
    }

    @Test
    void contextAssemblyLatency_FormatsContextUnder15Ms() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        RagContextBuilder contextBuilder = new RagContextBuilder(objectMapper);

        List<SearchResultItemResponse> items = new ArrayList<>();
        UUID docId = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            items.add(new SearchResultItemResponse(
                    UUID.randomUUID(),
                    docId,
                    "Chunk content block number " + i + " with factual data for RAG context builder performance test.",
                    0.95d - (i * 0.02d),
                    "{\"title\":\"Performance Doc\",\"originalFileName\":\"perf.pdf\",\"chunkIndex\":" + i + "}"
            ));
        }

        long startTime = System.nanoTime();
        RagContextBuilder.ContextBuildResult result = contextBuilder.buildContext(items, 4000);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        assertNotNull(result);
        assertEquals(10, result.getCitations().size());
        assertTrue(elapsedMs < 15, "Context assembly for 10 items should complete under 15ms. Measured: " + elapsedMs + "ms");
    }
}
