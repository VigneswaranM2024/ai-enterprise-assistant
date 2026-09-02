package com.enterprise.assistant.service.ratelimit.impl;

import com.enterprise.assistant.config.ratelimit.RateLimitProperties;
import com.enterprise.assistant.service.ratelimit.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Implementation of RateLimiterService backed by Redis atomic counter keys.
 * Enforces per-tenant, per-user, and per-endpoint request limits with 60-second fixed window counters.
 * Degrades gracefully on Redis connectivity failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterServiceImpl implements RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Override
    public boolean isAllowed(UUID tenantId, UUID userId, String endpointPath) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String sanitizedEndpoint = endpointPath != null ? endpointPath.replaceAll("[^a-zA-Z0-0_/-]", "_") : "unknown";
        String key = String.format("rate_limit:tenant:%s:user:%s:endpoint:%s",
                tenantId != null ? tenantId : "anonymous",
                userId != null ? userId : "anonymous",
                sanitizedEndpoint
        );

        return isAllowed(key);
    }

    @Override
    public boolean isAllowed(String key) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW_DURATION);
            }

            int limit = Math.max(1, rateLimitProperties.getRequestsPerMinute());
            if (count != null && count > limit) {
                log.warn("Rate limit exceeded for key '{}'. Current requests: {}, Limit: {}", key, count, limit);
                return false;
            }

            return true;
        } catch (Exception ex) {
            log.warn("Redis Rate Limiter unavailable: {}. Gracefully allowing request.", ex.getMessage());
            // Fallback to allow request when Redis is offline
            return true;
        }
    }
}
