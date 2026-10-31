package com.enterprise.assistant.exception;

import com.enterprise.assistant.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that unexpected server-side failures (for example a persistence outage) never leak
 * exception text, SQL state, or credentials to API consumers.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unhandledException_ReturnsGenericMessageWithoutInternalDetails() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleGenericException(
                new RuntimeException("SQL state: db_password=supersecret at ChatServiceImpl.persist"));

        assertEquals(500, response.getStatusCode().value());
        ApiResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("An unexpected internal server error occurred", body.getMessage());
        assertFalse(body.getMessage().contains("supersecret"));
        assertFalse(body.getMessage().contains("SQL"));
        assertNull(body.getData());
    }
}
