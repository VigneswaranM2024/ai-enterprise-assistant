package com.enterprise.assistant.eval.rag;

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
import com.enterprise.assistant.service.rag.RagContextBuilder;
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

/**
 * Quality Evaluation Tests for RAG generation, no-context short-circuit,
 * similarity threshold enforcement, and Groq LLM fault tolerance.
 */
@ExtendWith(MockitoExtension.class)
class RagQualityEvaluationTest {

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

        testTenant = Tenant.builder().id(tenantId).name("Quality Tenant").slug("q-tenant").build();
        testUser = User.builder().id(userId).tenant(testTenant).email("tester@qtenant.com").fullName("QA User").build();

        ragProperties = new RagProperties();
        ragProperties.getRetrieval().setTopK(5);
        ragProperties.getRetrieval().setMaxTopK(10);
        ragProperties.getRetrieval().setSimilarityThreshold(0.65);
        ragProperties.getRetrieval().setMaxContextTokens(4000);

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
    void ragAnswerGeneration_UsesRetrievedContext() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        SearchResultItemResponse item = new SearchResultItemResponse(chunkId, docId, "Employees receive 20 days paid leave.", 0.88d, "{\"title\":\"Leave Policy\"}");

        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), eq("Annual leave policy"), eq(5)))
                .thenReturn(new SemanticSearchResponse("Annual leave policy", 1, List.of(item)));

        CitationDTO citation = new CitationDTO("S1", docId, chunkId, "Leave Policy", "leave.pdf", 1, 0.88d);
        when(ragContextBuilder.buildContext(anyList(), anyInt()))
                .thenReturn(new RagContextBuilder.ContextBuildResult("<documents>[S1] Employees receive 20 days paid leave.</documents>", List.of(citation)));

        when(llmService.generateResponse(anyString(), anyString()))
                .thenReturn("Employees are entitled to 20 days paid leave per year [S1].");

        RagResponse response = ragService.generateAnswer(tenantId, userId, "Annual leave policy", 5);

        assertNotNull(response);
        assertTrue(response.answer().contains("20 days paid leave"));
        assertEquals(1, response.citations().size());
        assertEquals("S1", response.citations().get(0).citationId());

        verify(llmService).generateResponse(anyString(), anyString());
    }

    @Test
    void noContextRequest_DoesNotCallGroqLLM() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), eq("Unknown topic"), eq(5)))
                .thenReturn(new SemanticSearchResponse("Unknown topic", 0, Collections.emptyList()));

        RagResponse response = ragService.generateAnswer(tenantId, userId, "Unknown topic", 5);

        assertNotNull(response);
        assertTrue(response.answer().contains("couldn't find sufficiently relevant information"));
        assertTrue(response.citations().isEmpty());
        assertEquals(0, response.sourcesUsed());

        // LLM must NOT be invoked when no context matches
        verify(llmService, never()).generateResponse(anyString(), anyString());
    }

    @Test
    void lowSimilarityResults_FilteredOutBeforeGroqCall() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Score 0.40 is below configured similarity-threshold 0.65
        SearchResultItemResponse lowItem = new SearchResultItemResponse(UUID.randomUUID(), UUID.randomUUID(), "Low relevance text", 0.40d, null);
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), eq("Strict threshold query"), eq(5)))
                .thenReturn(new SemanticSearchResponse("Strict threshold query", 1, List.of(lowItem)));

        RagResponse response = ragService.generateAnswer(tenantId, userId, "Strict threshold query", 5);

        assertNotNull(response);
        assertTrue(response.answer().contains("couldn't find sufficiently relevant information"));

        verify(llmService, never()).generateResponse(anyString(), anyString());
    }

    @Test
    void groqFailure_LogsAuditFailureAndThrowsException() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        SearchResultItemResponse item = new SearchResultItemResponse(UUID.randomUUID(), UUID.randomUUID(), "Valid chunk text", 0.90d, null);
        when(semanticSearchService.searchAuthorized(any(SearchAuthorizationContext.class), anyString(), anyInt()))
                .thenReturn(new SemanticSearchResponse("query", 1, List.of(item)));

        when(ragContextBuilder.buildContext(anyList(), anyInt()))
                .thenReturn(new RagContextBuilder.ContextBuildResult("<documents>Content</documents>", Collections.emptyList()));

        when(llmService.generateResponse(anyString(), anyString()))
                .thenThrow(new RuntimeException("Groq API 500 Service Unavailable"));

        assertThrows(RuntimeException.class, () -> ragService.generateAnswer(tenantId, userId, "Error test", 5));

        verify(auditLogService).logEvent(eq(testTenant), eq(testUser), eq("RAG_FAILED"), eq("RAG"), any(), contains("Groq API 500"), eq("FAILURE"));
    }
}
