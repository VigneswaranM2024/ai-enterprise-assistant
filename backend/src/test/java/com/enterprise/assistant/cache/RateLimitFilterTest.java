package com.enterprise.assistant.cache;

import com.enterprise.assistant.domain.user.Role;
import com.enterprise.assistant.security.ratelimit.RateLimitFilter;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.ratelimit.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Filter Security Tests verifying HTTP 429 Too Many Requests responses,
 * UserPrincipal identity extraction, and non-bypassable rate limiting on expensive endpoints.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;
    private ObjectMapper objectMapper;

    private UUID tenantId;
    private UUID userId;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        rateLimitFilter = new RateLimitFilter(rateLimiterService, objectMapper);

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        userPrincipal = new UserPrincipal(
                userId, tenantId, "test-tenant", "rate@test.com", "pass", "Rate User", "INTERNAL", "ENG",
                true, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void rateLimitAllowed_ProceedsThroughFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ai/rag/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.isAllowed(eq(tenantId), eq(userId), eq("/api/v1/ai/rag/chat"))).thenReturn(true);

        rateLimitFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rateLimitExceeded_Returns429TooManyRequestsWithoutCallingFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ai/rag/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.isAllowed(eq(tenantId), eq(userId), eq("/api/v1/ai/rag/chat"))).thenReturn(false);

        rateLimitFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Rate limit exceeded"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void nonRateLimitedEndpoint_PassesThroughWithoutCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/documents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(rateLimiterService, never()).isAllowed(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }
}
