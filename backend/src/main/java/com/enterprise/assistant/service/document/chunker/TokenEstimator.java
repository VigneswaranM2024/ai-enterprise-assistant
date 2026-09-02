package com.enterprise.assistant.service.document.chunker;

/**
 * Interface for estimating token counts of text inputs prior to actual model tokenization.
 */
public interface TokenEstimator {

    /**
     * Estimates the number of tokens in the given text string.
     *
     * @param text Raw or processed text
     * @return Estimated token count
     */
    int estimateTokens(String text);
}
