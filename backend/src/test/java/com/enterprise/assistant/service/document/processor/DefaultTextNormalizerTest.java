package com.enterprise.assistant.service.document.processor;

import com.enterprise.assistant.service.document.processor.impl.DefaultTextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTextNormalizerTest {

    private DefaultTextNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new DefaultTextNormalizer();
    }

    @Test
    void normalize_NullOrEmpty_ReturnsEmptyString() {
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize("   "));
    }

    @Test
    void normalize_ExcessiveWhitespaceAndNewlines_NormalizesWhitespaceAndPreservesParagraphs() {
        String input = "Header   Title\n\n\n\nFirst   paragraph   content.\n\n\nSecond   paragraph content.\0";
        String expected = "Header Title\n\nFirst paragraph content.\n\nSecond paragraph content.";

        String result = normalizer.normalize(input);
        assertEquals(expected, result);
    }

    @Test
    void normalize_ControlCharactersAndCarriageReturns_StripsControlCharsAndUnifiesLineFeeds() {
        String input = "Line 1\r\nLine 2\rLine 3\u0007\u0008";
        String expected = "Line 1\nLine 2\nLine 3";

        String result = normalizer.normalize(input);
        assertEquals(expected, result);
    }
}
