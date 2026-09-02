package com.enterprise.assistant.service.search.impl;

import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
import com.enterprise.assistant.service.search.SearchAuthorizationContext;
import com.enterprise.assistant.service.search.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of SemanticSearchService providing both unauthenticated (tenant-only)
 * and fully authorized similarity search over pgvector.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    @Override
    @Transactional(readOnly = true)
    public SemanticSearchResponse search(UUID tenantId, String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        log.debug("Executing tenant-only semantic search for tenant: {}, topK: {}", tenantId, topK);

        List<Float> queryVector = embeddingService.generateEmbedding(query, GeminiTaskType.RETRIEVAL_QUERY);
        String vectorStr = formatVectorToString(queryVector);

        List<Object[]> rawResults = documentChunkRepository.findSimilarChunksNative(tenantId, vectorStr, Math.max(1, topK));

        List<SearchResultItemResponse> items = mapRawResults(rawResults);
        log.info("Semantic search completed for tenant: {} (Found {} matching chunks)", tenantId, items.size());
        return new SemanticSearchResponse(query, items.size(), items);
    }

    @Override
    @Transactional(readOnly = true)
    public SemanticSearchResponse searchAuthorized(SearchAuthorizationContext authCtx, String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        if (authCtx == null) {
            throw new IllegalArgumentException("Authorization context must not be null");
        }

        log.debug("Executing authorized semantic search for tenant: {}, clearance: {}, topK: {}",
                authCtx.getTenantId(), authCtx.getUserClearance(), topK);

        // Generate RETRIEVAL_QUERY embedding
        List<Float> queryVector = embeddingService.generateEmbedding(query, GeminiTaskType.RETRIEVAL_QUERY);
        String vectorStr = formatVectorToString(queryVector);

        // Format roles as a PostgreSQL text array literal: {ROLE_ADMIN,ROLE_EMPLOYEE}
        String rolesArrayLiteral = formatRolesArray(authCtx.getUserRoles());

        List<Object[]> rawResults = documentChunkRepository.findSimilarChunksAuthorized(
                authCtx.getTenantId(),
                vectorStr,
                Math.max(1, topK),
                authCtx.getUserClearance(),
                rolesArrayLiteral,
                authCtx.getUserDepartmentCode()
        );

        List<SearchResultItemResponse> items = mapRawResults(rawResults);
        log.info("Authorized semantic search completed for tenant: {} (Found {} authorized chunks out of top-{} candidates)",
                authCtx.getTenantId(), items.size(), topK);
        return new SemanticSearchResponse(query, items.size(), items);
    }

    // ---- Private helpers ----

    private List<SearchResultItemResponse> mapRawResults(List<Object[]> rawResults) {
        List<SearchResultItemResponse> items = new ArrayList<>(rawResults.size());
        for (Object[] row : rawResults) {
            UUID chunkId    = (UUID)   row[0];
            UUID documentId = (UUID)   row[1];
            String content  = (String) row[2];
            Double score    = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            String metadata = (String) row[4];
            items.add(new SearchResultItemResponse(chunkId, documentId, content, score, metadata));
        }
        return items;
    }

    private String formatVectorToString(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            sb.append(vector.get(i));
            if (i < vector.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Converts a list of role names into a PostgreSQL text array literal string.
     * Example: ["ROLE_ADMIN", "ROLE_EMPLOYEE"] → "{ROLE_ADMIN,ROLE_EMPLOYEE}"
     */
    private String formatRolesArray(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "{}";
        }
        return "{" + String.join(",", roles) + "}";
    }
}
