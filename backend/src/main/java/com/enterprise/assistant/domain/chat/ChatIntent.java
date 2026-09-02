package com.enterprise.assistant.domain.chat;

/**
 * Defines the possible intents for a user's chat message to route to the correct pipeline.
 */
public enum ChatIntent {
    /**
     * Casual conversation, greetings, acknowledgements.
     */
    CASUAL_CHAT,
    
    /**
     * Questions requiring specific enterprise knowledge or document search.
     */
    ENTERPRISE_KNOWLEDGE,
    
    /**
     * Requests asking to list, show, or describe available documents.
     */
    DOCUMENT_LIST,
    
    /**
     * Questions specifically about meetings, transcripts, decisions, or action items.
     */
    MEETING_QUERY,
    
    /**
     * Fallback intent if the model cannot classify securely.
     */
    UNKNOWN
}
