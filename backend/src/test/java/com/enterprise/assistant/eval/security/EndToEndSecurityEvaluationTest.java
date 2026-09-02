package com.enterprise.assistant.eval.security;

import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
import com.enterprise.assistant.service.search.SearchAuthorizationContext;
import com.enterprise.assistant.service.search.impl.SemanticSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * End-to-End Security Evaluation Tests verifying the 4 authorization boundaries:
 * 1. Tenant Isolation
 * 2. Security Classification Clearance
 * 3. Role Restrictions
 * 4. Department Restrictions
 */
@ExtendWith(MockitoExtension.class)
class EndToEndSecurityEvaluationTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    private SemanticSearchServiceImpl searchService;

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        searchService = new SemanticSearchServiceImpl(embeddingService, documentChunkRepository);
    }

    private void stubEmbedding() {
        when(embeddingService.generateEmbedding(anyString(), eq(GeminiTaskType.RETRIEVAL_QUERY)))
                .thenReturn(new ArrayList<>(Collections.nCopies(768, 0.1f)));
    }

    @Test
    void boundary1_TenantIsolation_TenantACannotAccessTenantB() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext ctx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse resp = searchService.searchAuthorized(ctx, "tenant B data query", 5);
        assertEquals(0, resp.totalResults());

        verify(documentChunkRepository).findSimilarChunksAuthorized(eq(TENANT_A), anyString(), eq(5), anyString(), anyString(), any());
        verify(documentChunkRepository, never()).findSimilarChunksAuthorized(eq(TENANT_B), anyString(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    void boundary2_SecurityClassification_LowerClearanceUserDeniedRestrictedChunk() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_A), anyString(), anyInt(), eq("INTERNAL"), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext internalUserCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse resp = searchService.searchAuthorized(internalUserCtx, "Top Secret Document", 5);
        assertEquals(0, resp.totalResults());

        verify(documentChunkRepository).findSimilarChunksAuthorized(eq(TENANT_A), anyString(), eq(5), eq("INTERNAL"), anyString(), any());
    }

    @Test
    void boundary3_RoleRestrictions_EmployeeDeniedAdminOnlyChunk() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_A), anyString(), anyInt(), anyString(), eq("{ROLE_EMPLOYEE}"), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext empCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("CONFIDENTIAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse resp = searchService.searchAuthorized(empCtx, "Admin Salary Table", 5);
        assertEquals(0, resp.totalResults());

        verify(documentChunkRepository).findSimilarChunksAuthorized(eq(TENANT_A), anyString(), eq(5), eq("CONFIDENTIAL"), eq("{ROLE_EMPLOYEE}"), any());
    }

    @Test
    void boundary4_DepartmentRestrictions_EngineeringUserDeniedHrDepartmentChunk() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), eq("ENGINEERING")))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext engCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode("ENGINEERING")
                .build();

        SemanticSearchResponse resp = searchService.searchAuthorized(engCtx, "HR Performance Reviews", 5);
        assertEquals(0, resp.totalResults());

        verify(documentChunkRepository).findSimilarChunksAuthorized(eq(TENANT_A), anyString(), eq(5), anyString(), anyString(), eq("ENGINEERING"));
    }
}
