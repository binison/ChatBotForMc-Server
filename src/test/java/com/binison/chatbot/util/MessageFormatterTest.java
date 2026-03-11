package com.binison.chatbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageFormatterTest {
    @Test
    void splitForChatTruncatesLongContent() {
        String content = "a".repeat(1300);
        List<String> lines = MessageFormatter.splitForChat("&b[AI]&r ", content, 1000);
        assertFalse(lines.isEmpty());
        assertTrue(lines.get(0).contains("[AI]"));
    }

    @Test
    void colorizeHandlesNull() {
        assertEquals("", MessageFormatter.colorize(null));
    }

    private void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}

