package com.enterprise.assistant.config.groq;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring Configuration Bean providing RestClient tailored for Groq LLM API requests.
 */
@Configuration
@RequiredArgsConstructor
public class GroqConfig {

    private final GroqProperties groqProperties;

    @Bean
    public RestClient groqRestClient() {
        String apiKey = groqProperties.getApiKey() != null ? groqProperties.getApiKey() : "";
        return RestClient.builder()
                .baseUrl(groqProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
