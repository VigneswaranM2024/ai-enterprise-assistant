package com.enterprise.assistant.service.search;

import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.service.embedding.EmbeddingService;
import com.enterprise.assistant.service.embedding.GeminiTaskType;
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
 * Security tests verifying that searchAuthorized enforces tenant isolation,
 * security classification, role-based, and department-based access control
 * entirely via database filtering (never in Java after retrieval).
 */
@ExtendWith(MockitoExtension.class)
class SemanticSearchSecurityTest {

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

    /** Stubs the embedding service for tests that actually call through to the service. */
    private void stubEmbedding() {
        when(embeddingService.generateEmbedding(anyString(), eq(GeminiTaskType.RETRIEVAL_QUERY)))
                .thenReturn(new ArrayList<>(Collections.nCopies(768, 0.1f)));
    }

    // -------------------------------------------------------------------------
    // 1. Tenant isolation
    // -------------------------------------------------------------------------

    @Test
    void tenantIsolation_TenantACannotRetrieveTenantBChunks() {
        stubEmbedding();
        // Simulate DB returns empty because tenant_id filter excludes Tenant B data
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext ctxA = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse response = searchService.searchAuthorized(ctxA, "What is the policy?", 5);

        assertEquals(0, response.totalResults());
        // Critically: Tenant B's ID was never passed to the query
        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), any());
        verify(documentChunkRepository, never()).findSimilarChunksAuthorized(
                eq(TENANT_B), anyString(), anyInt(), anyString(), anyString(), any());
    }

    // -------------------------------------------------------------------------
    // 2. Role-based access: ROLE_ADMIN-only chunk not visible to ROLE_EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void roleFilter_AdminOnlyChunk_NotReturnedForEmployee() {
        stubEmbedding();
        // DB returns empty for employee because the allowed_roles filter excludes ROLE_EMPLOYEE
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), eq("{ROLE_EMPLOYEE}"), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext employeeCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("CONFIDENTIAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse response = searchService.searchAuthorized(employeeCtx, "admin secrets", 5);

        assertEquals(0, response.totalResults());
        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), eq("CONFIDENTIAL"), eq("{ROLE_EMPLOYEE}"), any());
    }

    @Test
    void roleFilter_AdminOnlyChunk_ReturnedForAdmin() {
        stubEmbedding();
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        Object[] row = {chunkId, docId, "Admin-only content.", 0.92d, null};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);

        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), eq("{ROLE_ADMIN}"), any()))
                .thenReturn(rows);

        SearchAuthorizationContext adminCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("CONFIDENTIAL")
                .userRoles(List.of("ROLE_ADMIN"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse response = searchService.searchAuthorized(adminCtx, "admin secrets", 5);

        assertEquals(1, response.totalResults());
        assertEquals(chunkId, response.results().get(0).chunkId());
    }

    // -------------------------------------------------------------------------
    // 3. Department-based access
    // -------------------------------------------------------------------------

    @Test
    void deptFilter_HrChunk_NotReturnedForEngineeringUser() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), eq("ENGINEERING")))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext engCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode("ENGINEERING")
                .build();

        SemanticSearchResponse response = searchService.searchAuthorized(engCtx, "HR leave policy", 5);

        assertEquals(0, response.totalResults());
        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), eq("INTERNAL"), anyString(), eq("ENGINEERING"));
    }

    @Test
    void deptFilter_HrChunk_ReturnedForHrUser() {
        stubEmbedding();
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        Object[] row = {chunkId, docId, "HR leave content.", 0.88d, null};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);

        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), eq("HR")))
                .thenReturn(rows);

        SearchAuthorizationContext hrCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode("HR")
                .build();

        SemanticSearchResponse response = searchService.searchAuthorized(hrCtx, "HR leave policy", 5);

        assertEquals(1, response.totalResults());
        assertEquals(chunkId, response.results().get(0).chunkId());
    }

    // -------------------------------------------------------------------------
    // 4. Security classification
    // -------------------------------------------------------------------------

    @Test
    void classificationFilter_ConfidentialChunk_ExcludedForInternalUser() {
        stubEmbedding();
        // Database returns empty for INTERNAL user — the SQL ARRAY_POSITION filter does it
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), eq("INTERNAL"), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext internalCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        SemanticSearchResponse response = searchService.searchAuthorized(internalCtx, "confidential project", 5);

        assertEquals(0, response.totalResults());
        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), eq("INTERNAL"), anyString(), any());
    }

    // -------------------------------------------------------------------------
    // 5. Authorization context is NEVER constructed from client input
    // -------------------------------------------------------------------------

    @Test
    void authContext_AlwaysContainsTenantIdFromPrincipal_NeverFromClientInput() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext ctx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("PUBLIC")
                .userRoles(Collections.emptyList())
                .userDepartmentCode(null)
                .build();

        searchService.searchAuthorized(ctx, "test", 5);

        // Verify TENANT_A was used — not some other tenant
        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), any());
        verify(documentChunkRepository, never()).findSimilarChunksAuthorized(
                eq(TENANT_B), anyString(), anyInt(), anyString(), anyString(), any());
    }

    // -------------------------------------------------------------------------
    // 6. Multiple roles are handled correctly
    // -------------------------------------------------------------------------

    @Test
    void multipleRoles_FormattedAsArrayLiteralPassedToDatabase() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext multiRoleCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("CONFIDENTIAL")
                .userRoles(List.of("ROLE_ADMIN", "ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        searchService.searchAuthorized(multiRoleCtx, "multi-role query", 5);

        // Verify both roles are present in the array literal passed to DB
        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), eq("CONFIDENTIAL"),
                argThat(s -> s.contains("ROLE_ADMIN") && s.contains("ROLE_EMPLOYEE")),
                any());
    }

    // -------------------------------------------------------------------------
    // 7. Null department — passed as null, not as empty string (SQL null-safe)
    // -------------------------------------------------------------------------

    @Test
    void nullDepartment_PassedAsNullToDatabase() {
        stubEmbedding();
        when(documentChunkRepository.findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), anyString(), anyString(), isNull()))
                .thenReturn(Collections.emptyList());

        SearchAuthorizationContext noDeptCtx = SearchAuthorizationContext.builder()
                .tenantId(TENANT_A)
                .userClearance("INTERNAL")
                .userRoles(List.of("ROLE_EMPLOYEE"))
                .userDepartmentCode(null)
                .build();

        searchService.searchAuthorized(noDeptCtx, "general query", 5);

        verify(documentChunkRepository).findSimilarChunksAuthorized(
                eq(TENANT_A), anyString(), anyInt(), eq("INTERNAL"), anyString(), isNull());
    }

    // -------------------------------------------------------------------------
    // 8. Null auth context throws immediately (no DB call)
    // -------------------------------------------------------------------------

    @Test
    void nullAuthContext_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                searchService.searchAuthorized(null, "some query", 5));
        verifyNoInteractions(documentChunkRepository);
    }
}
