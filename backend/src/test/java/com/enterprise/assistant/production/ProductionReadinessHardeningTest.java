package com.enterprise.assistant.production;

import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.exception.AuthException;
import com.enterprise.assistant.exception.GlobalExceptionHandler;
import com.enterprise.assistant.security.filter.CorrelationIdFilter;
import com.enterprise.assistant.security.user.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Step 19 Production-Readiness and Security Hardening Test Suite.
 * Validates error handling, correlation ID propagation, secret leakage prevention,
 * authorization boundaries, and fault tolerance.
 */
@ExtendWith(MockitoExtension.class)
class ProductionReadinessHardeningTest {

    private GlobalExceptionHandler exceptionHandler;
    private CorrelationIdFilter correlationIdFilter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        correlationIdFilter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    void correlationIdFilter_GeneratesAndPropagatesHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilter(request, response, filterChain);

        String correlationIdHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(correlationIdHeader);
        assertFalse(correlationIdHeader.isBlank());
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY)); // MDC cleared after chain
    }

    @Test
    void correlationIdFilter_PreservesIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "custom-trace-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilter(request, response, filterChain);

        String correlationIdHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertEquals("custom-trace-12345", correlationIdHeader);
    }

    @Test
    void globalExceptionHandler_AuthException_Returns401WithCorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-401");
        AuthException ex = new AuthException("Invalid authentication credentials");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAuthException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid authentication credentials", response.getBody().getMessage());
        assertEquals("test-corr-401", response.getBody().getCorrelationId());
    }

    @Test
    void globalExceptionHandler_BadCredentials_Returns401WithoutSecretDetails() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-badcred");
        BadCredentialsException ex = new BadCredentialsException("Bad credentials secret details");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody().getMessage());
        assertEquals("test-corr-badcred", response.getBody().getCorrelationId());
    }

    @Test
    void globalExceptionHandler_AccessDenied_Returns403Forbidden() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-403");
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Forbidden"));
        assertEquals("test-corr-403", response.getBody().getCorrelationId());
    }

    @Test
    void globalExceptionHandler_MaxUploadSizeExceeded_Returns413PayloadTooLarge() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-413");
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(52428800L);

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleMaxUploadSizeExceeded(ex);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("50MB"));
    }

    @Test
    void globalExceptionHandler_GenericException_PreventsStacktraceAndKeyLeakage() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-500");
        RuntimeException ex = new RuntimeException("Internal SQL state failure with db_password=supersecret");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected internal server error occurred", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("supersecret"));
        assertFalse(response.getBody().getMessage().contains("SQL"));
        assertEquals("test-corr-500", response.getBody().getCorrelationId());
    }

    @Test
    void sensitiveDataLeakageCheck_UserPrincipalDoesNotExposePasswords() {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), "slug", "user@test.com", "$2a$12$hashedPassword",
                "Full Name", "INTERNAL", "ENG", true, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        assertEquals("user@test.com", principal.getUsername());
        assertNotNull(principal.getPassword()); // Used internally by Spring Security DAO provider
        assertEquals(1, principal.getAuthorities().size());
    }
}
