package com.enterprise.assistant.service.chat.intent;

import com.enterprise.assistant.domain.chat.ChatIntent;
import com.enterprise.assistant.service.ai.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service to classify the user's intent for chat routing.
 *
 * <p>Uses a two-stage approach:
 * <ol>
 *   <li>Fast deterministic pre-filter for obvious cases (greetings, document-list keywords,
 *       follow-up pronouns that are not about enterprise content) – avoids unnecessary LLM API calls.</li>
 *   <li>LLM-based semantic classification for everything else, with a safe fallback that keeps
 *       enterprise-flavoured questions on the grounded RAG path instead of ungrounded general
 *       knowledge when the intent is ambiguous.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentClassifierService {

    private final LlmService llmService;

    // -----------------------------------------------------------------------
    // Deterministic pre-filter patterns
    // -----------------------------------------------------------------------

    /** Short greeting phrases that are unambiguously conversational. */
    private static final Set<String> GREETING_EXACT = Set.of(
            "hi", "hello", "hey", "hiya", "howdy", "greetings", "sup", "yo",
            "good morning", "good afternoon", "good evening", "good night",
            "how are you", "how r u", "how are you doing", "what's up", "whats up",
            "how's it going", "hows it going", "nice to meet you", "pleased to meet you",
            "thanks", "thank you", "thank you so much", "thanks a lot", "ok", "okay",
            "got it", "understood", "sure", "great", "cool", "awesome", "perfect",
            "bye", "goodbye", "see you", "see ya", "take care", "have a good day"
    );

    /** Patterns that strongly indicate a DOCUMENT_LIST request. */
    private static final Pattern DOCUMENT_LIST_PATTERN = Pattern.compile(
            "(?i)\\b(list|show|enumerate|display|what|which|how many)\\b.{0,40}\\b(documents?|files?|uploads?|knowledge base)\\b(?!\\s+(say|says|contain|mention|describe|explain|about|related))",
            Pattern.CASE_INSENSITIVE
    );

    /** Follow-up indicators: references to prior conversation context. */
    private static final Pattern FOLLOW_UP_PATTERN = Pattern.compile(
            "(?i)^(tell me more|explain (more|that|it|the|this|your|previous|last|above|that)|"
            + "give (me )?(more|an example|five|three|some|additional)|elaborate|"
            + "what (do you mean|did you mean)|clarify|can you (explain|elaborate|give|describe|provide)|"
            + "more (detail|info|information|context)|and (what|how|why|where|who)|"
            + "what about|how about|why is (that|it|this)|the (first|second|third|fourth|fifth|last|previous|next) (point|item|one)|"
            + "explain (the )?(first|second|third|fourth|fifth|last|previous|next)|"
            + "continue|go on|proceed|what else|anything else|in (simpler|simple|layman|plain) (terms|words|language)|"
            + "simplify|rephrase|summarize (that|it|this|your)|give (me )?(a )?(summary|overview|example|code)|"
            + "show (me )?(a )?(code|example|snippet)|with an example|provide an example).*",
            Pattern.CASE_INSENSITIVE
    );

    /** Technical topic keywords that clearly route to TECHNICAL_EXPLANATION. */
    private static final Pattern TECHNICAL_PATTERN = Pattern.compile(
            "(?i)\\b(java|spring boot|spring|hibernate|jpa|rest api|microservices?|"
            + "docker|kubernetes|git|maven|gradle|junit|mockito|lombok|"
            + "sql|postgresql|mysql|mongodb|redis|elasticsearch|pgvector|vector database|"
            + "python|javascript|typescript|react|angular|vue|node\\.?js|"
            + "machine learning|ml|deep learning|neural network|llm|large language model|"
            + "rag|retrieval.augmented generation|embedding|transformer|"
            + "algorithm|data structure|design pattern|solid principles?|"
            + "dependency injection|inversion of control|orm|crud|http|rest|graphql|"
            + "ci/cd|devops|agile|scrum|microservice|api gateway|load balancer|"
            + "jwt|oauth|authentication|authorization|encryption|ssl|tls|"
            + "thread|concurrency|asynchronous|async|lambda|stream|functional programming|"
            + "class|interface|abstract|inheritance|polymorphism|encapsulation|"
            + "big o|complexity|recursion|sorting|searching|binary tree|graph)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /** Phrases that strongly indicate a DOCUMENT_SUMMARY request. */
    private static final Pattern DOCUMENT_SUMMARY_PATTERN = Pattern.compile(
            "(?i)\\b(summarize|summarise|summary|give (me )?(a )?summary|overview) \\b.{0,30}\\b(document|policy|handbook|guide|report|file|uploaded)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // -----------------------------------------------------------------------
    // LLM Prompt
    // -----------------------------------------------------------------------

    private static final String INTENT_SYSTEM_PROMPT = """
            You are an intent classification engine for an Enterprise AI Assistant.
            Your ONLY job is to classify the user's most recent message into exactly one of the following categories:

            CASUAL_CHAT: Greetings (e.g., "hi", "hello"), acknowledgments ("thanks", "ok", "got it"),
                         compliments, or purely social conversation with no information request.

            GENERAL_KNOWLEDGE: General factual or educational questions that do NOT require enterprise
                         documents. Examples: "What is machine learning?", "How does DNS work?",
                         "Explain the difference between TCP and UDP", "What is a database?",
                         "Who invented the internet?", "What is the capital of France?"

            TECHNICAL_EXPLANATION: Programming, software engineering, frameworks, databases, AI/ML,
                         DevOps, or computer science questions. Examples: "Explain Java OOP",
                         "What is Spring Boot?", "How does pgvector work?", "Explain RAG architecture",
                         "Compare PostgreSQL and Redis", "What is dependency injection?",
                         "Explain microservices", "How does JWT authentication work?"

            ENTERPRISE_KNOWLEDGE: Questions requiring information from the organization's uploaded
                         enterprise documents (policies, handbooks, company procedures). Examples:
                         "What does our leave policy say?", "Summarize our security policy",
                         "What does the employee handbook say about remote work?",
                         "According to our documents, what is the vacation policy?"

            DOCUMENT_LIST: Requests to enumerate or check which documents are available.
                         Examples: "List my uploaded documents", "What files are in the knowledge base?",
                         "Show me available documents", "How many documents have been uploaded?"
                         Do NOT select this if the user is asking about the CONTENT inside a document.

            DOCUMENT_SUMMARY: Requests to summarise a specific uploaded enterprise document.
                         Examples: "Summarize the employee handbook", "Give me an overview of the security policy"

            FOLLOW_UP: Questions that clearly reference the previous answer or conversation context.
                         Examples: "Tell me more about that", "Explain the second point",
                         "Give me an example", "Elaborate on that", "In simpler terms?",
                         "What did you mean by that?", "Can you clarify?",
                         "Give me five important points from what you said"

            MEETING_QUERY: Questions specifically about meetings, transcripts, decisions, or action items.
                         Examples: "What was discussed in the marketing sync?",
                         "Show my meeting action items", "Summarize the last team meeting"

            CRITICAL RULES:
            - TECHNICAL_EXPLANATION takes priority over ENTERPRISE_KNOWLEDGE for programming/tech questions.
            - GENERAL_KNOWLEDGE takes priority over ENTERPRISE_KNOWLEDGE for factual questions with no company context.
            - Do NOT classify as ENTERPRISE_KNOWLEDGE just because the question is detailed or complex.
            - Do NOT classify as DOCUMENT_LIST if the user asks about document CONTENT.
            - If genuinely uncertain between ENTERPRISE_KNOWLEDGE and GENERAL_KNOWLEDGE, choose GENERAL_KNOWLEDGE.
            - If genuinely uncertain between TECHNICAL_EXPLANATION and ENTERPRISE_KNOWLEDGE, choose TECHNICAL_EXPLANATION.
            - Questions about the organization's own documents, policies, handbooks, or procedures
              (for example "Can you explain our leave policy?") are ENTERPRISE_KNOWLEDGE and must
              never be classified as GENERAL_KNOWLEDGE, even when phrased as "explain".

            Respond with exactly ONE word from: CASUAL_CHAT, GENERAL_KNOWLEDGE, TECHNICAL_EXPLANATION,
            ENTERPRISE_KNOWLEDGE, DOCUMENT_LIST, DOCUMENT_SUMMARY, FOLLOW_UP, MEETING_QUERY

            Do NOT add punctuation, explanation, or any other text.
            """;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Classifies the intent of the given user message using deterministic pre-filters first,
     * then falling back to LLM-based classification for ambiguous cases.
     *
     * @param userMessage   the raw user message text
     * @param hasHistory    whether there is prior conversation history in this session
     * @return the classified {@link ChatIntent}, never null
     */
    public ChatIntent classifyIntent(String userMessage, boolean hasHistory) {
        if (userMessage == null || userMessage.isBlank()) {
            return ChatIntent.UNKNOWN;
        }

        String normalised = userMessage.trim().toLowerCase();

        // --- Stage 1: Deterministic exact-match pre-filter ---

        // Exact greeting match (very short messages)
        if (normalised.length() <= 80 && GREETING_EXACT.contains(normalised)) {
            log.debug("Pre-filter matched CASUAL_CHAT for: '{}'", userMessage);
            return ChatIntent.CASUAL_CHAT;
        }

        // Document list
        if (DOCUMENT_LIST_PATTERN.matcher(userMessage).find()) {
            log.debug("Pre-filter matched DOCUMENT_LIST for: '{}'", userMessage);
            return ChatIntent.DOCUMENT_LIST;
        }

        // Document summary
        if (DOCUMENT_SUMMARY_PATTERN.matcher(userMessage).find()) {
            log.debug("Pre-filter matched DOCUMENT_SUMMARY for: '{}'", userMessage);
            return ChatIntent.DOCUMENT_SUMMARY;
        }

        // Follow-up — only if there is prior history to reference and the message is not clearly
        // about enterprise documents or policies. "Can you explain our leave policy?" must stay on
        // the grounded RAG path rather than being hijacked as a follow-up.
        if (hasHistory
                && !containsEnterpriseIndicator(normalised)
                && FOLLOW_UP_PATTERN.matcher(userMessage).find()) {
            log.debug("Pre-filter matched FOLLOW_UP for: '{}'", userMessage);
            return ChatIntent.FOLLOW_UP;
        }

        // Technical explanation — only if message is short (clearly a definition/explanation request)
        // and does NOT include enterprise-specific words
        if (userMessage.length() <= 200
                && TECHNICAL_PATTERN.matcher(userMessage).find()
                && !containsEnterpriseIndicator(normalised)) {
            log.debug("Pre-filter matched TECHNICAL_EXPLANATION for: '{}'", userMessage);
            return ChatIntent.TECHNICAL_EXPLANATION;
        }

        // --- Stage 2: LLM-based classification ---
        return classifyWithLlm(userMessage, normalised);
    }

    /**
     * Backward-compatible overload that assumes no history exists.
     * Used for single-turn tests or where history is unavailable.
     */
    public ChatIntent classifyIntent(String userMessage) {
        return classifyIntent(userMessage, false);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private boolean containsEnterpriseIndicator(String normalised) {
        return normalised.contains("our ") || normalised.contains("company")
                || normalised.contains("organization") || normalised.contains("organisation")
                || normalised.contains("policy") || normalised.contains("handbook")
                || normalised.contains("uploaded") || normalised.contains("document")
                || normalised.contains("enterprise") || normalised.contains("corporate")
                || normalised.contains("employee") || normalised.contains("department");
    }

    private ChatIntent classifyWithLlm(String userMessage, String normalised) {
        // Deterministic safety net: if the model is unreachable or answers with an unknown label,
        // an enterprise-flavoured question must not silently fall back to ungrounded general
        // knowledge — grounding it through RAG (and possibly refusing) is the safer failure mode.
        ChatIntent fallback = containsEnterpriseIndicator(normalised)
                ? ChatIntent.ENTERPRISE_KNOWLEDGE
                : ChatIntent.GENERAL_KNOWLEDGE;
        try {
            log.debug("LLM classifying intent for: '{}'", userMessage);
            String rawResponse = llmService.generateResponse(INTENT_SYSTEM_PROMPT, userMessage);
            if (rawResponse != null && !rawResponse.isBlank()) {
                String intentStr = rawResponse.trim().toUpperCase().replaceAll("[^A-Z_]", "");
                try {
                    ChatIntent intent = ChatIntent.valueOf(intentStr);
                    log.debug("LLM classified intent as {} for: '{}'", intent, userMessage);
                    return intent;
                } catch (IllegalArgumentException e) {
                    log.warn("LLM returned unrecognised intent '{}' for message: '{}'. Defaulting to {}.",
                            intentStr, userMessage, fallback);
                    return fallback;
                }
            }
        } catch (Exception e) {
            log.error("Intent classification LLM call failed. Defaulting to {}.", fallback, e);
        }
        // Safe default — never ENTERPRISE_KNOWLEDGE for inputs with no enterprise signal
        return fallback;
    }
}
