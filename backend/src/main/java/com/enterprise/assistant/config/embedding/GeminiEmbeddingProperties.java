package com.enterprise.assistant.config.embedding;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Properties for Google Gemini Embedding Integration.
 * Binds properties prefixed with 'embedding'.
 */
@Configuration
@ConfigurationProperties(prefix = "embedding")
@Getter
@Setter
@Slf4j
public class GeminiEmbeddingProperties {

    private String provider = "gemini";
    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String model = "models/gemini-embedding-2";
    private int dimension = 768;
    private int batchSize = 50;

    @PostConstruct
    public void validate() {
        if ("gemini".equalsIgnoreCase(provider) && (apiKey == null || apiKey.isBlank())) {
            log.warn("Gemini Embedding API key is missing. GEMINI_API_KEY environment variable should be configured.");
        }
        log.info("Initialized Gemini Embedding Configuration (Model: {}, Dimension: {}, Batch Size: {})", model, dimension, batchSize);
    }
}
