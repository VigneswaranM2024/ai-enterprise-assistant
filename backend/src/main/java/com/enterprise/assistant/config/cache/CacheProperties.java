package com.enterprise.assistant.config.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Production Redis Cache Configuration Properties.
 * Controls cache enablement and per-domain explicit TTLs.
 */
@Configuration
@ConfigurationProperties(prefix = "cache.redis")
@Getter
@Setter
public class CacheProperties {

    private boolean enabled = true;
    private Duration defaultTtl = Duration.ofMinutes(10);
    private Duration documentMetadataTtl = Duration.ofMinutes(10);
    private Duration permissionTtl = Duration.ofMinutes(5);
    private Duration sessionTtl = Duration.ofMinutes(15);
    private Duration dashboardTtl = Duration.ofMinutes(2);
    private Duration platformDashboardTtl = Duration.ofMinutes(2);
}
