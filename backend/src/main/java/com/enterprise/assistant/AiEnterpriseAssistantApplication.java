package com.enterprise.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Enterprise Assistant — Spring Boot Application Entry Point.
 *
 * Multi-tenant cognitive search platform leveraging Java 21,
 * Spring Boot 3.3+, Spring Security, and PostgreSQL with pgvector.
 */
@SpringBootApplication
@EnableAsync
public class AiEnterpriseAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEnterpriseAssistantApplication.class, args);
    }
}
