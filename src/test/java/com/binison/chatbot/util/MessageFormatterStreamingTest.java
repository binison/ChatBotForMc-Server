package com.binison.chatbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageFormatterStreamingTest {
    @Test
    void consumeStreamableSegmentsWaitsUntilThresholdUnlessForced() {
        StringBuilder pending = new StringBuilder("你好世界");

        List<String> notEnough = MessageFormatter.consumeStreamableSegments("&d[米糯]&r ", pending, 0, 10, false);
        assertTrue(notEnough.isEmpty());
        assertEquals("你好世界", pending.toString());

        List<String> forced = MessageFormatter.consumeStreamableSegments("&d[米糯]&r ", pending, 0, 10, true);
        assertEquals(1, forced.size());
        assertTrue(forced.getFirst().contains("米糯"));
        assertTrue(forced.getFirst().contains("你好世界"));
        assertEquals(0, pending.length());
    }

    @Test
    void consumeStreamableSegmentsPrefersNaturalBoundary() {
        StringBuilder pending = new StringBuilder("你好，世界继续");

        List<String> lines = MessageFormatter.consumeStreamableSegments("", pending, 0, 4, false);

        assertEquals(List.of("你好，"), lines);
        assertEquals("世界继续", pending.toString());
    }
}

