package com.enterprise.assistant.config.rag;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for RAG retrieval, context filtering, and generation.
 * Binds properties prefixed with 'rag'.
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RagProperties {

    private Retrieval retrieval = new Retrieval();
    private Generation generation = new Generation();

    @Getter
    @Setter
    public static class Retrieval {
        private int topK = 5;
        private int maxTopK = 10;
        private double similarityThreshold = 0.65;
        private int maxContextTokens = 6000;
    }

    @Getter
    @Setter
    public static class Generation {
        private double temperature = 0.2;
        private int maxTokens = 2048;
    }
}
