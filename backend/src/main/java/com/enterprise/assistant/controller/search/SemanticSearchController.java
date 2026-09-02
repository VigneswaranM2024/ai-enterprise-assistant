package com.enterprise.assistant.controller.search;

import com.enterprise.assistant.dto.request.SemanticSearchRequest;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.search.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterprise.assistant.service.search.SearchAuthorizationContext;

/**
 * REST Controller exposing tenant-isolated, access-controlled semantic similarity search endpoints.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Semantic Search", description = "Endpoints for tenant-isolated vector similarity search over enterprise knowledge chunks")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @PostMapping("/semantic")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @Operation(summary = "Semantic similarity search", description = "Executes vector cosine similarity search over tenant document chunks using Gemini RETRIEVAL_QUERY embeddings")
    public ResponseEntity<SemanticSearchResponse> search(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody SemanticSearchRequest request
    ) {
        SearchAuthorizationContext authCtx = SearchAuthorizationContext.fromUserPrincipal(currentUser);
        SemanticSearchResponse response = semanticSearchService.searchAuthorized(
                authCtx,
                request.query(),
                request.getEffectiveTopK()
        );

        return ResponseEntity.ok(response);
    }
}
