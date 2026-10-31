package com.enterprise.assistant.domain.chat;

/**
 * Defines the possible intents for a user's chat message to route to the correct pipeline.
 * Expanded to cover general knowledge, technical explanations, follow-up questions,
 * and document summarisation in addition to enterprise RAG queries.
 */
public enum ChatIntent {
    /**
     * Casual conversation, greetings, acknowledgements, small talk.
     */
    CASUAL_CHAT,

    /**
     * General educational, factual, or informational questions not requiring company documents.
     * Example: "What is machine learning?", "How does HTTP work?"
     */
    GENERAL_KNOWLEDGE,

    /**
     * Technical programming, framework, database, AI or software engineering questions.
     * Example: "Explain Spring Boot dependency injection", "What is pgvector?", "Compare SQL vs NoSQL"
     */
    TECHNICAL_EXPLANATION,

    /**
     * Questions requiring specific enterprise knowledge or document search.
     * Example: "What does our leave policy say?", "Explain the security policy"
     */
    ENTERPRISE_KNOWLEDGE,

    /**
     * Requests asking to list, show, or describe available documents.
     * Example: "List my uploaded documents", "What files are in the knowledge base?"
     */
    DOCUMENT_LIST,

    /**
     * Requests to summarise one or more uploaded enterprise documents.
     * Example: "Summarize the employee handbook", "Give me a summary of the security policy"
     */
    DOCUMENT_SUMMARY,

    /**
     * Follow-up questions referencing a previous answer, point, or context.
     * Example: "Tell me more about that", "Explain the second point", "Give me an example"
     */
    FOLLOW_UP,

    /**
     * Questions specifically about meetings, transcripts, decisions, or action items.
     */
    MEETING_QUERY,

    /**
     * Fallback intent if the model cannot classify securely.
     */
    UNKNOWN
}
