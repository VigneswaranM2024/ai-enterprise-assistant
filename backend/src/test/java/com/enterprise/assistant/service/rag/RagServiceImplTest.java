package com.enterprise.assistant.service.rag;

import com.enterprise.assistant.config.groq.GroqProperties;
import com.enterprise.assistant.config.rag.RagProperties;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.dto.response.CitationDTO;
import com.enterprise.assistant.dto.response.RagResponse;
import com.enterprise.assistant.dto.response.SearchResultItemResponse;
import com.enterprise.assistant.dto.response.SemanticSearchResponse;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.ai.LlmService;
import com.enterprise.assistant.service.audit.AuditLogService;
import com.enterprise.assistant.service.rag.impl.RagServiceImpl;
import com.enterprise.assistant.service.search.SearchAuthorizationContext;
import com.enterprise.assistant.service.search.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private LlmService llmService;

    @Mock
    private RagContextBuilder ragContextBuilder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    private RagProperties ragProperties;
    private GroqProperties groqProperties;
    private RagServiceImpl ragService;

    private Tenant testTenant;
    private User testUser;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testTenant = Tenant.builder().id(tenantId).name("Acme Corp").slug("acme").build();
        testUser = User.builder().id(userId).tenant(testTenant).email("admin@acme.com").fullName("Admin User").build();

        ragProperties = new RagProperties();
        ragProperties.getRetrieval().setTopK(5);
        ragProperties.getRetrieval().setMaxTopK(10);
        ragProperties.getRetrieval().setSimilarityThreshold(0.65);
        ragProperties.getRetrieval().setMaxContextTokens(6000);

        groqProperties = new GroqProperties();
        groqProperties.setModel("openai/gpt-oss-20b");

        ragService = new RagServiceImpl(
                semanticSearchService,
                llmService,
                ragContextBuilder,
                auditLogService,
                tenantRepository,
                userRepository,
                ragProperties,
                groqProperties
        );
    }

    @Test
    void generateAnswer_NullQuery_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ragService.generateAnswer(tenantId, userId, "", 5));
    }

    @Test
    void generateAnswer_NoSearchHits_ReturnsControlledMessageWithoutCallingLlm() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // searchAuthorized is called (no Security context → fallback to PUBLIC clearance, no roles)
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), anyString(), eq(5)))
                .thenReturn(new SemanticSearchResponse("Leave Policy?", 0, Collections.emptyList()));

        RagResponse response = ragService.generateAnswer(tenantId, userId, "Leave Policy?", 5);

        assertNotNull(response);
        assertTrue(response.answer().contains("couldn't find sufficiently relevant information"));
        assertTrue(response.citations().isEmpty());
        assertEquals(0, response.sourcesUsed());

        verify(llmService, never()).generateResponse(anyString(), anyString());
    }

    @Test
    void generateAnswer_LowSimilarityHits_FiltersOutAndDoesNotCallLlm() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Score 0.40 is below configured threshold of 0.65
        SearchResultItemResponse lowItem = new SearchResultItemResponse(UUID.randomUUID(), UUID.randomUUID(), "Irrelevant text", 0.40d, null);
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), anyString(), eq(5)))
                .thenReturn(new SemanticSearchResponse("Leave Policy?", 1, List.of(lowItem)));

        RagResponse response = ragService.generateAnswer(tenantId, userId, "Leave Policy?", 5);

        assertNotNull(response);
        assertTrue(response.answer().contains("couldn't find sufficiently relevant information"));
        assertTrue(response.citations().isEmpty());

        verify(llmService, never()).generateResponse(anyString(), anyString());
    }

    @Test
    void generateAnswer_ValidHighSimilarityHits_CallsLlmAndReturnsCitations() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        SearchResultItemResponse highItem = new SearchResultItemResponse(
                chunkId, docId, "Employees apply for annual leave via portal.", 0.88d, "{\"title\":\"Leave Policy\"}"
        );
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), anyString(), eq(5)))
                .thenReturn(new SemanticSearchResponse("How to request leave?", 1, List.of(highItem)));

        CitationDTO mockCitation = new CitationDTO("S1", docId, chunkId, "Leave Policy", "leave.pdf", 1, 0.88d);
        when(ragContextBuilder.buildContext(anyList(), anyInt()))
                .thenReturn(new RagContextBuilder.ContextBuildResult("<documents>[S1] Leave Policy...</documents>", List.of(mockCitation)));

        when(llmService.generateResponse(anyString(), anyString()))
                .thenReturn("Employees apply for annual leave via the HR portal [S1].");

        RagResponse response = ragService.generateAnswer(tenantId, userId, "How to request leave?", 5);

        assertNotNull(response);
        assertEquals("Employees apply for annual leave via the HR portal [S1].", response.answer());
        assertEquals(1, response.citations().size());
        assertEquals("S1", response.citations().get(0).citationId());

        verify(llmService).generateResponse(anyString(), anyString());
        verify(auditLogService, times(2)).logEvent(eq(testTenant), eq(testUser), anyString(), eq("RAG"), any(), anyString(), eq("SUCCESS"));
    }

    @Test
    void generateAnswer_TopKExceedsMax_LimitsToMaxTopK() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), anyString(), eq(10)))
                .thenReturn(new SemanticSearchResponse("Test", 0, Collections.emptyList()));

        // Pass 999 as topK (max allowed is 10)
        ragService.generateAnswer(tenantId, userId, "Test query", 999);

        verify(semanticSearchService).searchAuthorized(any(SearchAuthorizationContext.class), eq("Test query"), eq(10));
    }

    @Test
    void generateAnswer_UsesSearchAuthorized_NeverCallsUnsearchMethod() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(semanticSearchService.searchAuthorized(any(), anyString(), anyInt()))
                .thenReturn(new SemanticSearchResponse("q", 0, Collections.emptyList()));

        ragService.generateAnswer(tenantId, userId, "test query", 5);

        // Verify the authorized overload was called, NOT the unauthenticated one
        verify(semanticSearchService, atLeastOnce()).searchAuthorized(any(SearchAuthorizationContext.class), anyString(), anyInt());
        verify(semanticSearchService, never()).search(any(UUID.class), anyString(), anyInt());
    }

    @Test
    void generateAnswer_UnauthorizedChunksNeverReachContextBuilder() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // DB already filtered: only 0 authorized results
        when(semanticSearchService.searchAuthorized(any(), anyString(), anyInt()))
                .thenReturn(new SemanticSearchResponse("q", 0, Collections.emptyList()));

        ragService.generateAnswer(tenantId, userId, "restricted document content", 5);

        // Context builder must never be called with unauthorized data
        verify(ragContextBuilder, never()).buildContext(anyList(), anyInt());
    }
}
