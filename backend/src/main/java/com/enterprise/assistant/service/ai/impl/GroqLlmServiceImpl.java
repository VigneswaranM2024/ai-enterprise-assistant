package com.enterprise.assistant.service.ai.impl;

import com.enterprise.assistant.config.groq.GroqProperties;
import com.enterprise.assistant.dto.ai.groq.GroqChatRequest;
import com.enterprise.assistant.dto.ai.groq.GroqChatResponse;
import com.enterprise.assistant.dto.ai.groq.GroqMessage;
import com.enterprise.assistant.service.ai.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Implementation of LlmService integrating with Groq OpenAI-compatible API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqLlmServiceImpl implements LlmService {

    private final RestClient groqRestClient;
    private final GroqProperties groqProperties;

    @Override
    public String generateResponse(String userPrompt) {
        return generateResponse("You are an AI Enterprise Assistant platform assistant.", userPrompt);
    }

    @Override
    public String generateResponse(String systemPrompt, String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt must not be empty");
        }

        if (groqProperties.getApiKey() == null || groqProperties.getApiKey().isBlank()) {
            log.error("Groq LLM invocation aborted: GROQ_API_KEY environment variable is not configured.");
            throw new IllegalStateException("GROQ_API_KEY environment variable must be set and non-empty for Groq LLM integration.");
        }

        GroqChatRequest request = new GroqChatRequest(
                groqProperties.getModel(),
                List.of(
                        new GroqMessage("system", systemPrompt != null ? systemPrompt : "You are a helpful assistant."),
                        new GroqMessage("user", userPrompt)
                ),
                groqProperties.getTemperature(),
                groqProperties.getMaxTokens()
        );

        log.debug("Dispatching chat completion request to Groq API (Model: {})", groqProperties.getModel());

        try {
            ResponseEntity<GroqChatResponse> responseEntity = groqRestClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .toEntity(GroqChatResponse.class);

            GroqChatResponse responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.choices() == null || responseBody.choices().isEmpty()) {
                log.warn("Received empty choices array from Groq LLM response.");
                throw new IllegalStateException("Groq API returned an empty completion response.");
            }

            String content = responseBody.choices().get(0).message().content();
            if (content == null || content.isBlank()) {
                log.warn("Received blank text content from Groq LLM response choice.");
                return "";
            }

            log.debug("Groq LLM response successfully generated (Length: {} chars)", content.length());
            return content;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("Groq API HTTP Error - Status: {}, Message: {}", ex.getStatusCode(), ex.getStatusText());
            throw new RuntimeException("Groq API call failed with status " + ex.getStatusCode() + ": " + ex.getStatusText(), ex);
        } catch (Exception ex) {
            log.error("Failed to execute Groq LLM API completion call: {}", ex.getMessage());
            throw new RuntimeException("Groq API invocation failed: " + ex.getMessage(), ex);
        }
    }
}
