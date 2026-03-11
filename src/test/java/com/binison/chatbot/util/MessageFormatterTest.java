package com.binison.chatbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageFormatterTest {
    @Test
    void splitForChatTruncatesLongContent() {
        String content = "a".repeat(1300);
        List<String> lines = MessageFormatter.splitForChat("&d[米糯]&r ", content, 1000);
        assertFalse(lines.isEmpty());
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().contains("米糯"));
    }

    @Test
    void splitForChatDoesNotTruncateWhenDisabled() {
        String content = "a".repeat(1300);
        List<String> lines = MessageFormatter.splitForChat("&d[米糯]&r ", content, 0);
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().length() >= content.length());
    }

    @Test
    void colorizeHandlesNull() {
        assertEquals("", MessageFormatter.colorize(null));
    }
}
