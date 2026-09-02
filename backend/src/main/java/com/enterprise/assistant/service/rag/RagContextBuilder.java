package com.enterprise.assistant.service.rag;

import com.enterprise.assistant.dto.response.CitationDTO;
import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Component responsible for building controlled, token-bounded context blocks
 * and authoritative citation metadata mapping from retrieved search items.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagContextBuilder {

    private final ObjectMapper objectMapper;

    @Getter
    public static class ContextBuildResult {
        private final String formattedContext;
        private final List<CitationDTO> citations;

        public ContextBuildResult(String formattedContext, List<CitationDTO> citations) {
            this.formattedContext = formattedContext;
            this.citations = citations;
        }
    }

    public ContextBuildResult buildContext(List<SearchResultItemResponse> items, int maxContextTokens) {
        if (items == null || items.isEmpty()) {
            return new ContextBuildResult("", List.of());
        }

        StringBuilder contextBuilder = new StringBuilder("<documents>\n");
        List<CitationDTO> citations = new ArrayList<>(items.size());
        int maxCharLength = maxContextTokens * 4; // Approx 4 chars per token
        int currentLength = 0;

        for (int i = 0; i < items.size(); i++) {
            SearchResultItemResponse item = items.get(i);
            String citationId = "S" + (i + 1);

            String title = "Enterprise Document";
            String fileName = "document.pdf";
            Integer chunkIndex = null;

            if (item.metadata() != null && !item.metadata().isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(item.metadata());
                    if (node.has("title") && !node.get("title").asText().isBlank()) {
                        title = node.get("title").asText();
                    }
                    if (node.has("originalFileName") && !node.get("originalFileName").asText().isBlank()) {
                        fileName = node.get("originalFileName").asText();
                    }
                } catch (Exception ex) {
                    log.warn("Failed to parse chunk metadata JSON for citation: {}", citationId, ex);
                }
            }

            CitationDTO citation = new CitationDTO(
                    citationId,
                    item.documentId(),
                    item.chunkId(),
                    title,
                    fileName,
                    chunkIndex,
                    item.score()
            );

            String block = String.format(
                    "[%s] Document: %s | File: %s\nContent: %s\n\n",
                    citationId,
                    title,
                    fileName,
                    item.content() != null ? item.content().trim() : ""
            );

            if (currentLength + block.length() > maxCharLength && i > 0) {
                log.info("Reached maximum context character boundary ({} chars). Truncating remaining chunks.", maxCharLength);
                break;
            }

            contextBuilder.append(block);
            citations.add(citation);
            currentLength += block.length();
        }

        contextBuilder.append("</documents>");
        return new ContextBuildResult(contextBuilder.toString(), citations);
    }
}
