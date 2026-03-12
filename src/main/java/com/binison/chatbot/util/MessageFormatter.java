package com.binison.chatbot.util;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;

public final class MessageFormatter {
    private MessageFormatter() {
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public static List<String> splitForChat(String prefix, String content, int maxOutputLength) {
        String safePrefix = prefix == null ? "" : colorize(prefix);
        String safeContent = sanitizeContent(content);
        if (maxOutputLength > 0 && safeContent.length() > maxOutputLength) {
            safeContent = safeContent.substring(0, maxOutputLength) + "...";
        }
        return List.of(safePrefix + safeContent);
    }

    public static String sanitizeContent(String content) {
        return content == null ? "" : content.replace('§', ' ');
    }

    public static List<String> consumeStreamableSegments(String prefix, StringBuilder pending, int maxOutputLength, int flushChars, boolean force) {
        List<String> lines = new ArrayList<>();
        if (pending == null || pending.isEmpty()) {
            return lines;
        }

        String safePrefix = prefix == null ? "" : colorize(prefix);
        int flushIndex = findFlushIndex(pending, Math.max(1, flushChars), force);
        if (flushIndex <= 0) {
            return lines;
        }

        String chunk = sanitizeContent(pending.substring(0, flushIndex));
        pending.delete(0, flushIndex);
        if (chunk.isBlank()) {
            return lines;
        }
        if (maxOutputLength > 0 && chunk.length() > maxOutputLength) {
            chunk = chunk.substring(0, maxOutputLength) + "...";
        }
        lines.add(safePrefix + chunk);
        return lines;
    }

    private static int findFlushIndex(StringBuilder pending, int flushChars, boolean force) {
        if (pending.length() < flushChars && !force) {
            return 0;
        }
        int upperBound = force ? pending.length() : Math.min(pending.length(), flushChars);
        for (int index = upperBound - 1; index >= 0; index--) {
            char current = pending.charAt(index);
            if (isNaturalBoundary(current)) {
                return index + 1;
            }
        }
        return force ? pending.length() : upperBound;
    }

    private static boolean isNaturalBoundary(char current) {
        return Character.isWhitespace(current) || "。！？!?；;，,、.\n".indexOf(current) >= 0;
    }
}
