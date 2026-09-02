package com.enterprise.assistant.service.document.processor.impl;

import com.enterprise.assistant.service.document.processor.TextNormalizer;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Default implementation of TextNormalizer.
 * Cleans extracted text for RAG readiness without destroying structural boundaries.
 */
@Component
public class DefaultTextNormalizer implements TextNormalizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[ \\t\\xA0]+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");

    @Override
    public String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        // 1. Remove null bytes and invalid control characters
        String text = CONTROL_CHARS.matcher(rawText).replaceAll("");

        // 2. Standardize carriage returns to standard linefeeds
        text = text.replace("\r\n", "\n").replace('\r', '\n');

        // 3. Trim horizontal whitespace on each line while preserving linebreaks
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = HORIZONTAL_WHITESPACE.matcher(lines[i]).replaceAll(" ").trim();
            sb.append(trimmedLine);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        // 4. Consolidate 3+ consecutive newlines down to max 2 (preserving paragraph breaks)
        String normalized = MULTIPLE_NEWLINES.matcher(sb.toString()).replaceAll("\n\n");

        return normalized.trim();
    }
}
