package com.binison.chatbot.util;

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
        if (maxOutputLength > 0 && safeContent.length() > maxOutputLength) {
            safeContent = safeContent.substring(0, maxOutputLength) + "...";
        }
        return List.of(safePrefix + safeContent);
    }
}
