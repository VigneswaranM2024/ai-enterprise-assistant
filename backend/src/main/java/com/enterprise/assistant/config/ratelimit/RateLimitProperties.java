package com.enterprise.assistant.config.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Production API Rate Limiting Configuration Properties.
 * Controls rate limiting enablement and requests per minute limits.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private int requestsPerMinute = 30;
}
