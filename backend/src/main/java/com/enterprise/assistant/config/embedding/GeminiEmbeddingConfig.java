package com.enterprise.assistant.config.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration class producing dedicated RestClient for Gemini Embedding API requests.
 */
@Configuration
@RequiredArgsConstructor
public class GeminiEmbeddingConfig {

    private final GeminiEmbeddingProperties embeddingProperties;

    @Bean
    public RestClient geminiEmbeddingRestClient() {
        return RestClient.builder()
                .baseUrl(embeddingProperties.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
