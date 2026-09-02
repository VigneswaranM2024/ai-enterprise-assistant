package com.enterprise.assistant.service.ratelimit;

import java.util.UUID;

/**
 * Service Contract for Redis-backed API Rate Limiting.
 * Evaluates tenant + user + endpoint request counters against configurable thresholds.
 */
public interface RateLimiterService {

    /**
     * Evaluates whether a request for a specific tenant, user, and endpoint is within allowed rate limits.
     *
     * @param tenantId     the authenticated tenant ID
     * @param userId       the authenticated user ID
     * @param endpointPath the target API path
     * @return true if request is permitted, false if limit exceeded
     */
    boolean isAllowed(UUID tenantId, UUID userId, String endpointPath);

    /**
     * Evaluates a custom rate limit key against default or custom request limits.
     *
     * @param key the tenant/user/endpoint rate limit key
     * @return true if allowed, false if limit exceeded
     */
    boolean isAllowed(String key);
}
