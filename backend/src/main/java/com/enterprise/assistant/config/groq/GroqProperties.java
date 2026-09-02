package com.enterprise.assistant.config.groq;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Properties for Groq LLM API integration.
 * Binds properties prefixed with 'groq'.
 */
@Configuration
@ConfigurationProperties(prefix = "groq")
@Getter
@Setter
@Slf4j
public class GroqProperties {

    /**
     * Groq API Key obtained from GROQ_API_KEY environment variable.
     */
    private String apiKey;

    /**
     * Groq OpenAI-compatible base URL.
     */
    private String baseUrl = "https://api.groq.com/openai/v1";

    /**
     * Configurable Groq model name. Default: openai/gpt-oss-20b
     */
    private String model = "openai/gpt-oss-20b";

    /**
     * Temperature parameter for generation (0.0 to 1.0). Default: 0.2
     */
    private double temperature = 0.2;

    /**
     * Maximum completion tokens. Default: 2048
     */
    private int maxTokens = 2048;

    @PostConstruct
    public void validate() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Groq configuration failure: GROQ_API_KEY environment variable is missing or blank");
            throw new IllegalStateException("GROQ_API_KEY environment variable must be set and non-empty for Groq LLM integration.");
        }
        log.info("Groq LLM Configuration initialized successfully. Base URL: {}, Model: {}", baseUrl, model);
    }
}
