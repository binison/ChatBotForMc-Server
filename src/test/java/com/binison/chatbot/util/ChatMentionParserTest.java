package com.binison.chatbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChatMentionParserTest {
    @Test
    void extractsPromptWhenMessageStartsWithMentionPrefix() {
        assertEquals("你好", ChatMentionParser.extractPrompt("@ai 你好", List.of("@ai")));
        assertEquals("hello there", ChatMentionParser.extractPrompt("@AI hello there", List.of("@ai")));
        assertEquals("怎么了", ChatMentionParser.extractPrompt("@ai: 怎么了", List.of("@ai")));
        assertEquals("我来了", ChatMentionParser.extractPrompt("@ai：我来了", List.of("@ai")));
    }

    @Test
    void returnsEmptyStringWhenMentionHasNoContent() {
        assertEquals("", ChatMentionParser.extractPrompt("@ai", List.of("@ai")));
        assertEquals("", ChatMentionParser.extractPrompt("@ai    ", List.of("@ai")));
        assertEquals("", ChatMentionParser.extractPrompt("@ai:", List.of("@ai")));
    }

    @Test
    void ignoresMessagesThatDoNotStartWithMentionPrefix() {
        assertNull(ChatMentionParser.extractPrompt("hello @ai", List.of("@ai")));
        assertNull(ChatMentionParser.extractPrompt("大家觉得@ai怎么样", List.of("@ai")));
    }

    @Test
    void supportsMultipleMentionPrefixes() {
        assertEquals("test", ChatMentionParser.extractPrompt("@bot test", List.of("@ai", "@bot")));
        assertEquals("你好呀", ChatMentionParser.extractPrompt("@米糯 你好呀", List.of("@ai", "@米糯")));
        assertEquals("来帮忙", ChatMentionParser.extractPrompt("@米糯: 来帮忙", List.of("@ai", "@米糯")));
    }
}
