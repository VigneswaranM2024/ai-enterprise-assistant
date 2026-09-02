package com.enterprise.assistant.controller.ai;

import com.enterprise.assistant.dto.request.CodeGenerationRequest;
import com.enterprise.assistant.dto.request.EmailGenerationRequest;
import com.enterprise.assistant.dto.request.SqlGenerationRequest;
import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.UtilityResponse;
import com.enterprise.assistant.service.ai.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/utilities")
@RequiredArgsConstructor
@Slf4j
public class AiUtilityController {

    private final LlmService llmService;

    private static final String EMAIL_SYSTEM_PROMPT = """
            You are an expert enterprise communicator.
            Generate a highly professional, well-structured email based on the user's context.
            Adhere strictly to the requested tone and target audience.
            Output ONLY the email subject and body. Do not include any explanations or pleasantries.
            """;

    private static final String SQL_SYSTEM_PROMPT = """
            You are an expert database administrator and SQL developer.
            Generate optimized, secure, and accurate SQL queries based on the user's prompt.
            Adhere strictly to the requested SQL dialect.
            Use the provided schema context if available.
            Output ONLY valid SQL code. If you must include an explanation or warning, wrap it in SQL comments (-- or /* */).
            """;

    private static final String CODE_SYSTEM_PROMPT = """
            You are a senior software engineer.
            Generate clean, efficient, and well-documented code based on the user's prompt.
            Adhere strictly to the requested programming language.
            Output ONLY the code block. Use standard markdown code blocks (e.g., ```java).
            Include comments to explain complex logic.
            """;

    @PostMapping("/email")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    public ResponseEntity<ApiResponse<UtilityResponse>> generateEmail(
            @Valid @RequestBody EmailGenerationRequest request) {
        
        String userPrompt = String.format("Context: %s\nTone: %s\nAudience: %s", 
                request.getContext(), request.getTone(), request.getTargetAudience());
        
        String result = llmService.generateResponse(EMAIL_SYSTEM_PROMPT, userPrompt);
        return ResponseEntity.ok(ApiResponse.success("Email generated", new UtilityResponse(result)));
    }

    @PostMapping("/sql")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    public ResponseEntity<ApiResponse<UtilityResponse>> generateSql(
            @Valid @RequestBody SqlGenerationRequest request) {
        
        String userPrompt = String.format("Prompt: %s\nDialect: %s\nSchema Context: %s", 
                request.getPrompt(), request.getDialect(), request.getSchemaContext());
        
        String result = llmService.generateResponse(SQL_SYSTEM_PROMPT, userPrompt);
        return ResponseEntity.ok(ApiResponse.success("SQL generated", new UtilityResponse(result)));
    }

    @PostMapping("/code")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    public ResponseEntity<ApiResponse<UtilityResponse>> generateCode(
            @Valid @RequestBody CodeGenerationRequest request) {
        
        String userPrompt = String.format("Prompt: %s\nLanguage: %s", 
                request.getPrompt(), request.getLanguage());
        
        String result = llmService.generateResponse(CODE_SYSTEM_PROMPT, userPrompt);
        return ResponseEntity.ok(ApiResponse.success("Code generated", new UtilityResponse(result)));
    }
}
