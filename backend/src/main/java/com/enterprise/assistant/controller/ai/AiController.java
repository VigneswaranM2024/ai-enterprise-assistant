package com.enterprise.assistant.controller.ai;

import com.enterprise.assistant.config.groq.GroqProperties;
import com.enterprise.assistant.dto.request.AiTestRequest;
import com.enterprise.assistant.dto.request.RagRequest;
import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.AiTestResponse;
import com.enterprise.assistant.dto.response.RagResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.ai.LlmService;
import com.enterprise.assistant.service.rag.RagService;
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

/**
 * REST Controller for AI operations, LLM integration testing, and RAG chat generation.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Services", description = "Endpoints for LLM generation and enterprise cognitive assistant operations")
public class AiController {

    private final LlmService llmService;
    private final RagService ragService;
    private final GroqProperties groqProperties;

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "Test Groq LLM integration", description = "Protected test endpoint sending a prompt to Groq LLM API and returning generated text response")
    public ResponseEntity<AiTestResponse> testGroqLlm(@Valid @RequestBody AiTestRequest request) {
        String generatedContent = llmService.generateResponse(request.prompt());
        AiTestResponse response = new AiTestResponse(generatedContent, groqProperties.getModel());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rag/chat")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "RAG Chat generation", description = "Tenant-isolated RAG pipeline generating grounded enterprise responses with source citations")
    public ResponseEntity<ApiResponse<RagResponse>> ragChat(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RagRequest request
    ) {
        RagResponse response = ragService.generateAnswer(
                currentUser.getTenantId(),
                currentUser.getId(),
                request.query(),
                request.topK()
        );
        return ResponseEntity.ok(ApiResponse.success("RAG generation completed successfully", response));
    }
}
