package com.enterprise.assistant.controller.search;

import com.enterprise.assistant.dto.request.SemanticSearchRequest;
import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.search.SearchAuthorizationContext;
import com.enterprise.assistant.service.search.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchControllerTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    private SemanticSearchController controller;

    private UUID tenantId;
    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;

    @BeforeEach
    void setUp() {
        controller = new SemanticSearchController(semanticSearchService);
        tenantId = UUID.randomUUID();

        adminPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                tenantId,
                "acme",
                "admin@acme.com",
                "hashedpwd",
                "Admin User",
                "CONFIDENTIAL",
                "EXECUTIVE",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("DOCUMENT_READ"))
        );

        employeePrincipal = new UserPrincipal(
                UUID.randomUUID(),
                tenantId,
                "acme",
                "emp@acme.com",
                "hashedpwd",
                "Emp User",
                "INTERNAL",
                "ENGINEERING",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"), new SimpleGrantedAuthority("DOCUMENT_READ"))
        );
    }

    @Test
    void search_AdminUser_BuildsAuthorizationContextAndDelegatesToSearchAuthorized() {
        SemanticSearchRequest request = new SemanticSearchRequest("quarterly report", 5);

        SearchResultItemResponse item = new SearchResultItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Confidential Q3 data", 0.91d, "{\"title\":\"Q3 Report\"}"
        );
        SemanticSearchResponse mockResponse = new SemanticSearchResponse("quarterly report", 1, List.of(item));

        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), eq("quarterly report"), eq(5)))
                .thenReturn(mockResponse);

        ResponseEntity<SemanticSearchResponse> responseEntity = controller.search(adminPrincipal, request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().totalResults());

        ArgumentCaptor<SearchAuthorizationContext> captor = ArgumentCaptor.forClass(SearchAuthorizationContext.class);
        verify(semanticSearchService).searchAuthorized(captor.capture(), eq("quarterly report"), eq(5));

        SearchAuthorizationContext authCtx = captor.getValue();
        assertEquals(tenantId, authCtx.getTenantId());
        assertEquals("CONFIDENTIAL", authCtx.getUserClearance());
        assertTrue(authCtx.getUserRoles().contains("ROLE_ADMIN"));
        assertEquals("EXECUTIVE", authCtx.getUserDepartmentCode());

        // Verify old unauthenticated search() is NEVER called
        verify(semanticSearchService, never()).search(any(), any(), anyInt());
    }

    @Test
    void search_EmployeeUser_BuildsContextWithEmployeeAttributes() {
        SemanticSearchRequest request = new SemanticSearchRequest("onboarding doc", 3);

        SemanticSearchResponse mockResponse = new SemanticSearchResponse("onboarding doc", 0, List.of());
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), eq("onboarding doc"), eq(3)))
                .thenReturn(mockResponse);

        ResponseEntity<SemanticSearchResponse> responseEntity = controller.search(employeePrincipal, request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ArgumentCaptor<SearchAuthorizationContext> captor = ArgumentCaptor.forClass(SearchAuthorizationContext.class);
        verify(semanticSearchService).searchAuthorized(captor.capture(), eq("onboarding doc"), eq(3));

        SearchAuthorizationContext authCtx = captor.getValue();
        assertEquals(tenantId, authCtx.getTenantId());
        assertEquals("INTERNAL", authCtx.getUserClearance());
        assertTrue(authCtx.getUserRoles().contains("ROLE_EMPLOYEE"));
        assertEquals("ENGINEERING", authCtx.getUserDepartmentCode());
    }
}
