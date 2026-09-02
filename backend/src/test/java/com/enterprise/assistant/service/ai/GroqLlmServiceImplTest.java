package com.enterprise.assistant.service.ai;

import com.enterprise.assistant.config.groq.GroqProperties;
import com.enterprise.assistant.dto.ai.groq.GroqChatRequest;
import com.enterprise.assistant.dto.ai.groq.GroqChatResponse;
import com.enterprise.assistant.dto.ai.groq.GroqMessage;
import com.enterprise.assistant.service.ai.impl.GroqLlmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroqLlmServiceImplTest {

    @Mock
    private RestClient groqRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GroqProperties groqProperties;
    private GroqLlmServiceImpl groqLlmService;

    @BeforeEach
    void setUp() {
        groqProperties = new GroqProperties();
        groqProperties.setApiKey("gsk_test_mock_api_key_12345");
        groqProperties.setBaseUrl("https://api.groq.com/openai/v1");
        groqProperties.setModel("openai/gpt-oss-20b");
        groqProperties.setTemperature(0.2);
        groqProperties.setMaxTokens(2048);

        groqLlmService = new GroqLlmServiceImpl(groqRestClient, groqProperties);
    }

    @Test
    void generateResponse_MissingApiKey_ThrowsIllegalStateException() {
        groqProperties.setApiKey("");
        assertThrows(IllegalStateException.class, () -> groqLlmService.generateResponse("Hello"));
    }

    @Test
    void generateResponse_NullUserPrompt_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> groqLlmService.generateResponse(null));
    }

    @Test
    void generateResponse_Success_ReturnsGeneratedText() {
        GroqChatResponse.Choice choice = new GroqChatResponse.Choice(0, new GroqMessage("assistant", "Hello! I am Groq AI."), "stop");
        GroqChatResponse mockResponse = new GroqChatResponse("chatcmpl-123", List.of(choice));

        when(groqRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GroqChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(GroqChatResponse.class)).thenReturn(ResponseEntity.ok(mockResponse));

        String result = groqLlmService.generateResponse("Introduce yourself");

        assertNotNull(result);
        assertEquals("Hello! I am Groq AI.", result);
        verify(groqRestClient).post();
    }

    @Test
    void generateResponse_EmptyChoices_ThrowsIllegalStateException() {
        GroqChatResponse mockResponse = new GroqChatResponse("chatcmpl-123", Collections.emptyList());

        when(groqRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GroqChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(GroqChatResponse.class)).thenReturn(ResponseEntity.ok(mockResponse));

        assertThrows(IllegalStateException.class, () -> groqLlmService.generateResponse("Hello"));
    }

    @Test
    void generateResponse_HttpError_ThrowsRuntimeExceptionWithoutExposingKey() {
        when(groqRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GroqChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientResponseException("Unauthorized", HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null, null, null));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> groqLlmService.generateResponse("Hello"));
        assertTrue(exception.getMessage().contains("401"));
        assertFalse(exception.getMessage().contains("gsk_test_mock_api_key_12345"));
    }
}
