package com.enterprise.assistant.service.document.chunker;

import com.enterprise.assistant.service.document.chunker.impl.DefaultTokenEstimator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTokenEstimatorTest {

    private DefaultTokenEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new DefaultTokenEstimator();
    }

    @Test
    void estimateTokens_NullOrEmpty_ReturnsZero() {
        assertEquals(0, estimator.estimateTokens(null));
        assertEquals(0, estimator.estimateTokens("   "));
    }

    @Test
    void estimateTokens_ValidText_ReturnsEstimatedCount() {
        String text = "This is a simple sentence for token estimation."; // 47 chars
        int tokens = estimator.estimateTokens(text);
        assertTrue(tokens > 0);
        assertEquals(12, tokens); // ceil(47/4) = 12
    }
}
