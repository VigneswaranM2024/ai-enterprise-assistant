package com.enterprise.assistant.service.document.chunker.impl;

import com.enterprise.assistant.service.document.chunker.TokenEstimator;
import org.springframework.stereotype.Component;

/**
 * Default implementation of TokenEstimator using standard character-to-token approximation heuristics.
 * Approximates 1 token per 4 characters (~0.75 words per token) for standard text.
 */
@Component
public class DefaultTokenEstimator implements TokenEstimator {

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Standard rule of thumb: ~4 characters per token
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
