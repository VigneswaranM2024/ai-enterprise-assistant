package com.enterprise.assistant.service.chat.intent;

import com.enterprise.assistant.domain.chat.ChatIntent;
import com.enterprise.assistant.service.ai.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to classify the user's intent for chat routing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentClassifierService {

    private final LlmService llmService;

    private static final String INTENT_SYSTEM_PROMPT = """
            You are an intent classification engine for an Enterprise AI Assistant.
            Your ONLY job is to classify the user's most recent message into exactly one of the following categories:

            CASUAL_CHAT: Greetings (e.g., "hi", "hello"), acknowledgments ("thanks"), or general casual conversation.
            DOCUMENT_LIST: Requests explicitly asking to enumerate, list, or check the existence of available documents or files (e.g., "What documents are available?", "List the documents in the knowledge base", "Show me available documents", "What files are uploaded?"). Do NOT select this if the user is asking about the content inside a document.
            ENTERPRISE_KNOWLEDGE: Questions that require looking up company policies, facts, or information contained INSIDE enterprise documents. Select this if the user asks what a document says, to explain information from a document, summarize a document, or asks about details/topics in a document (e.g., "What does the document say about X?", "Tell me everything the document says about Y", "Summarize the security document", "According to the document, what should employees do?"). The mere presence of words like "document", "file", or "policy" must NOT trigger DOCUMENT_LIST if the intent is to extract knowledge from them.
            MEETING_QUERY: Questions specifically about meetings, transcripts, decisions, or action items (e.g., "What was discussed in the marketing sync?", "Show my meeting action items").

            Analyze the user message and respond with exactly ONE word matching one of the above categories.
            Do NOT add punctuation, explanation, or any other text.
            If the intent is unclear, default to ENTERPRISE_KNOWLEDGE.
            """;

    public ChatIntent classifyIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return ChatIntent.UNKNOWN;
        }

        try {
            log.debug("Classifying intent for message: '{}'", userMessage);
            String rawResponse = llmService.generateResponse(INTENT_SYSTEM_PROMPT, userMessage);
            if (rawResponse != null) {
                String intentStr = rawResponse.trim().toUpperCase();
                
                // Sanitize potential extra characters from LLM
                intentStr = intentStr.replaceAll("[^A-Z_]", "");

                return ChatIntent.valueOf(intentStr);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse intent from LLM response. Defaulting to ENTERPRISE_KNOWLEDGE.", e);
            return ChatIntent.ENTERPRISE_KNOWLEDGE;
        } catch (Exception e) {
            log.error("Error during intent classification. Defaulting to ENTERPRISE_KNOWLEDGE.", e);
            return ChatIntent.ENTERPRISE_KNOWLEDGE;
        }

        return ChatIntent.ENTERPRISE_KNOWLEDGE;
    }
}
