package com.enterprise.assistant.service.document.chunker.impl;

import com.enterprise.assistant.domain.document.Document;
import com.enterprise.assistant.domain.document.DocumentChunk;
import com.enterprise.assistant.service.document.chunker.ChunkingOptions;
import com.enterprise.assistant.service.document.chunker.DocumentChunker;
import com.enterprise.assistant.service.document.chunker.TokenEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Semantic recursive document chunker that splits text hierarchically across:
 * 1. Paragraph boundaries (\n\n)
 * 2. Sentence boundaries (. ! ?)
 * 3. Word boundaries (\s+)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecursiveDocumentChunker implements DocumentChunker {

    private final TokenEstimator tokenEstimator;

    @Override
    public List<DocumentChunk> chunk(Document document, ChunkingOptions options) {
        if (document == null || document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            return Collections.emptyList();
        }

        String text = document.getExtractedText();
        int targetTokens = options.targetTokens();
        int overlapTokens = options.overlapTokens();

        List<String> rawChunks = buildRawChunks(text, targetTokens, overlapTokens);

        List<DocumentChunk> result = new ArrayList<>(rawChunks.size());
        String metadataJson = buildMetadataJson(document);

        for (int i = 0; i < rawChunks.size(); i++) {
            String chunkContent = rawChunks.get(i);
            int tokenCount = tokenEstimator.estimateTokens(chunkContent);

            DocumentChunk chunk = DocumentChunk.builder()
                    .tenant(document.getTenant())
                    .document(document)
                    .chunkIndex(i)
                    .content(chunkContent)
                    .characterCount(chunkContent.length())
                    .tokenEstimate(tokenCount)
                    .metadata(metadataJson)
                    .securityClassification(document.getSecurityClassification())
                    .allowedRoles(document.getAllowedRoles())
                    .allowedDepartments(document.getAllowedDepartments())
                    .build();

            result.add(chunk);
        }

        log.debug("Chunked document (ID: {}) into {} chunks", document.getId(), result.size());
        return result;
    }

    private List<String> buildRawChunks(String text, int targetTokens, int overlapTokens) {
        List<String> units = splitIntoUnits(text);
        List<String> chunks = new ArrayList<>();

        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (String unit : units) {
            int unitTokens = tokenEstimator.estimateTokens(unit);

            // If a single unit exceeds targetTokens, force split it by words/chars
            if (unitTokens > targetTokens) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                    currentTokens = 0;
                }
                List<String> subUnits = forceSubSplit(unit, targetTokens);
                for (String sub : subUnits) {
                    chunks.add(sub.trim());
                }
                continue;
            }

            if (currentTokens + unitTokens > targetTokens && currentChunk.length() > 0) {
                String chunkText = currentChunk.toString().trim();
                chunks.add(chunkText);

                // Build overlap from trailing text of currentChunk
                String overlapText = extractOverlap(chunkText, overlapTokens);
                currentChunk.setLength(0);
                if (!overlapText.isBlank()) {
                    currentChunk.append(overlapText).append(" ");
                    currentTokens = tokenEstimator.estimateTokens(overlapText);
                } else {
                    currentTokens = 0;
                }
            }

            currentChunk.append(unit).append(" ");
            currentTokens += unitTokens;
        }

        if (currentChunk.length() > 0 && !currentChunk.toString().isBlank()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitIntoUnits(String text) {
        List<String> units = new ArrayList<>();
        // Split by paragraphs first
        String[] paragraphs = text.split("\n\n+");
        for (String para : paragraphs) {
            String trimmedPara = para.trim();
            if (trimmedPara.isEmpty()) continue;

            if (tokenEstimator.estimateTokens(trimmedPara) <= 200) {
                units.add(trimmedPara);
            } else {
                // Split long paragraph by sentences
                String[] sentences = trimmedPara.split("(?<=[.!?])\\s+");
                for (String sentence : sentences) {
                    if (!sentence.isBlank()) {
                        units.add(sentence.trim());
                    }
                }
            }
        }
        return units;
    }

    private List<String> forceSubSplit(String text, int targetTokens) {
        List<String> result = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (tokenEstimator.estimateTokens(sb.toString() + " " + word) > targetTokens && sb.length() > 0) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            }
            sb.append(word).append(" ");
        }
        if (sb.length() > 0) {
            result.add(sb.toString().trim());
        }
        return result;
    }

    private String extractOverlap(String text, int overlapTokens) {
        if (overlapTokens <= 0 || text.isBlank()) {
            return "";
        }
        String[] words = text.split("\\s+");
        StringBuilder overlapSb = new StringBuilder();

        for (int i = words.length - 1; i >= 0; i--) {
            String candidate = words[i] + " " + overlapSb.toString();
            if (tokenEstimator.estimateTokens(candidate) > overlapTokens) {
                break;
            }
            overlapSb.insert(0, words[i] + " ");
        }
        return overlapSb.toString().trim();
    }

    private String buildMetadataJson(Document document) {
        String title = document.getTitle() != null ? document.getTitle().replace("\"", "\\\"") : "";
        String fileName = document.getOriginalFileName() != null ? document.getOriginalFileName().replace("\"", "\\\"") : "";
        String category = document.getCategory() != null ? document.getCategory().name() : "";
        String classification = document.getSecurityClassification() != null ? document.getSecurityClassification().name() : "";

        return String.format(
                "{\"documentId\":\"%s\",\"tenantId\":\"%s\",\"title\":\"%s\",\"originalFileName\":\"%s\",\"category\":\"%s\",\"securityClassification\":\"%s\",\"version\":%d}",
                document.getId(),
                document.getTenant().getId(),
                title,
                fileName,
                category,
                classification,
                document.getVersion()
        );
    }
}
