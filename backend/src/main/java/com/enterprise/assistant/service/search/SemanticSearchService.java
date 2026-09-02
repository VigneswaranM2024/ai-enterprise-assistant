package com.enterprise.assistant.service.search;

import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import java.util.UUID;

/**
 * Service contract for tenant-isolated semantic similarity search.
 */
public interface SemanticSearchService {

    /**
     * Performs a tenant-scoped vector similarity search over document chunks
     * with NO additional access-control filtering beyond tenant isolation.
     * <p>Used by the standalone {@code POST /api/v1/search/semantic} endpoint.</p>
     *
     * @param tenantId Tenant ID enforcing isolation
     * @param query    User search query
     * @param topK     Number of top matching results to retrieve
     * @return SemanticSearchResponse containing top matching chunks with similarity scores
     */
    SemanticSearchResponse search(UUID tenantId, String query, int topK);

    /**
     * Performs a fully authorized vector similarity search enforcing:
     * <ul>
     *   <li>Tenant isolation</li>
     *   <li>Security classification clearance</li>
     *   <li>Role-based access control (allowed_roles)</li>
     *   <li>Department-based access control (allowed_departments)</li>
     * </ul>
     * <p>Used by the RAG pipeline. Authorization context is built from the authenticated JWT principal.</p>
     *
     * @param authCtx Authorization context derived from the authenticated JWT — never from client request body
     * @param query   User search query
     * @param topK    Number of top matching results to retrieve
     * @return SemanticSearchResponse containing only chunks the caller is authorized to access
     */
    SemanticSearchResponse searchAuthorized(SearchAuthorizationContext authCtx, String query, int topK);
}
