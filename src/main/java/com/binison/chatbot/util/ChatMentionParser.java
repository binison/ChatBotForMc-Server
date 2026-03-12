package com.binison.chatbot.util;

import java.util.List;

public final class ChatMentionParser {
    private ChatMentionParser() {
    }

    public static String extractPrompt(String rawMessage, List<String> prefixes) {
        if (rawMessage == null || rawMessage.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return null;
        }

        String message = rawMessage.trim();
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            String normalizedPrefix = prefix.trim();
            if (message.equalsIgnoreCase(normalizedPrefix)) {
                return "";
            }
            if (message.regionMatches(true, 0, normalizedPrefix, 0, normalizedPrefix.length())) {
                if (message.length() == normalizedPrefix.length()) {
                    return "";
                }
                char nextChar = message.charAt(normalizedPrefix.length());
                if (Character.isWhitespace(nextChar)) {
                    return message.substring(normalizedPrefix.length()).trim();
                }
                if (nextChar == ':' || nextChar == '：') {
                    return message.substring(normalizedPrefix.length() + 1).trim();
                }
            }
        }

        return null;
    }
}
