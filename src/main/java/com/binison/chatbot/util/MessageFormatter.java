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
        String safeContent = content == null ? "" : content.replace('§', ' ');
        if (safeContent.length() > maxOutputLength) {
            safeContent = safeContent.substring(0, maxOutputLength) + "...";
        }
        List<String> lines = new ArrayList<>();
        int chunk = 240;
        for (int i = 0; i < safeContent.length(); i += chunk) {
            lines.add(safePrefix + safeContent.substring(i, Math.min(safeContent.length(), i + chunk)));
        }
        if (lines.isEmpty()) {
            lines.add(safePrefix);
        }
        return lines;
    }
}

