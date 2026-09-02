package com.enterprise.assistant.service.rag;

import com.enterprise.assistant.dto.response.RagResponse;

import java.util.UUID;

/**
 * Service contract for executing RAG answer generation with verified source citations.
 */
public interface RagService {

    /**
     * Executes RAG semantic retrieval, prompt context construction, Groq LLM completion, and citation mapping.
     *
     * @param tenantId Tenant ID enforcing multi-tenant isolation
     * @param userId Authenticated User ID performing the query
     * @param query User prompt string
     * @param topK Optional top-K chunk retrieval limit
     * @return RagResponse containing synthesized text, verified citations, and metadata
     */
    RagResponse generateAnswer(UUID tenantId, UUID userId, String query, Integer topK);
}
