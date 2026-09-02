package com.enterprise.assistant.security.ratelimit;

import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.ratelimit.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security Filter enforcing production rate limits on expensive AI, RAG, and Search endpoints.
 * Obtains tenantId and userId strictly from the authenticated UserPrincipal.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && isRateLimitedEndpoint(uri)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
                boolean allowed = rateLimiterService.isAllowed(principal.getTenantId(), principal.getId(), uri);
                if (!allowed) {
                    log.warn("Rate limit exceeded for user: {} (Tenant: {}) on endpoint: {}", principal.getId(), principal.getTenantId(), uri);
                    
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    ApiResponse<Void> apiResponse = ApiResponse.error("Rate limit exceeded. Please try again later.");
                    objectMapper.writeValue(response.getWriter(), apiResponse);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedEndpoint(String uri) {
        if (uri == null) return false;
        return uri.equals("/api/v1/ai/rag/chat")
                || uri.equals("/api/v1/search/semantic")
                || (uri.startsWith("/api/v1/chat/sessions/") && uri.endsWith("/messages"));
    }
}
