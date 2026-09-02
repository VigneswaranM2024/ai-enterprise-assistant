package com.enterprise.assistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Repository and Entity Auditing Configuration Bean.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.enterprise.assistant.repository")
public class JpaConfig {
}
