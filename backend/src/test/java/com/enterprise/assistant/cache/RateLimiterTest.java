package com.enterprise.assistant.cache;

import com.enterprise.assistant.config.ratelimit.RateLimitProperties;
import com.enterprise.assistant.service.ratelimit.impl.RateLimiterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Rate Limiting Unit & Isolation Tests verifying request counting, HTTP 429 enforcement,
 * tenant/user bucket isolation, disabled state, and Redis connection failure safety.
 */
@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RateLimitProperties properties;
    private RateLimiterServiceImpl rateLimiterService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setRequestsPerMinute(30);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiterService = new RateLimiterServiceImpl(redisTemplate, properties);
    }

    @Test
    void requestsBelowLimit_Succeeds() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        boolean allowed = rateLimiterService.isAllowed(tenantId, userId, "/api/v1/ai/rag/chat");

        assertTrue(allowed);
    }

    @Test
    void requestsExceedingLimit_Rejected() {
        when(valueOperations.increment(anyString())).thenReturn(31L);

        boolean allowed = rateLimiterService.isAllowed(tenantId, userId, "/api/v1/ai/rag/chat");

        assertFalse(allowed);
    }

    @Test
    void userAndTenantBucketIsolation_GeneratesDistinctRedisKeys() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(valueOperations.increment(contains(tenantA.toString()))).thenReturn(1L);
        when(valueOperations.increment(contains(tenantB.toString()))).thenReturn(1L);

        assertTrue(rateLimiterService.isAllowed(tenantA, userA, "/api/v1/search/semantic"));
        assertTrue(rateLimiterService.isAllowed(tenantB, userB, "/api/v1/search/semantic"));

        verify(valueOperations).increment(argThat(key -> key.contains(tenantA.toString()) && key.contains(userA.toString())));
        verify(valueOperations).increment(argThat(key -> key.contains(tenantB.toString()) && key.contains(userB.toString())));
    }

    @Test
    void disabledRateLimiting_AlwaysPermitsRequest() {
        properties.setEnabled(false);

        boolean allowed = rateLimiterService.isAllowed(tenantId, userId, "/api/v1/ai/rag/chat");

        assertTrue(allowed);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void redisUnavailable_GracefullyDegradesToAllowRequest() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis connection timeout"));

        boolean allowed = rateLimiterService.isAllowed(tenantId, userId, "/api/v1/ai/rag/chat");

        assertTrue(allowed, "Rate limiter must gracefully degrade and allow requests when Redis is unavailable");
    }
}
