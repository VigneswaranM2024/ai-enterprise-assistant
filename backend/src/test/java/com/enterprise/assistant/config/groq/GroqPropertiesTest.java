package com.enterprise.assistant.config.groq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the default completion-token budget used for Groq requests (free-tier TPM safety).
 */
class GroqPropertiesTest {

    @Test
    void defaultCompletionBudgetIs1024Tokens() {
        GroqProperties properties = new GroqProperties();

        assertEquals(1024, properties.getMaxTokens());
    }
}
