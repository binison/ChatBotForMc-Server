package com.binison.chatbot.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.service.ChatService;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class AiCommandTest {
    @Test
    void plainAiMessageNowShowsMentionHintInsteadOfStartingChat() {
        ChatBotPlugin plugin = mock(ChatBotPlugin.class);
        ChatService chatService = mock(ChatService.class);
        AiCommand command = new AiCommand(plugin, chatService);
        Player player = mock(Player.class);
        Command bukkitCommand = mock(Command.class);

        command.onCommand(player, bukkitCommand, "ai", new String[]{"hello"});

        verify(player).sendMessage("Use @ai <message> in chat to talk with the assistant.");
        verify(chatService, never()).handleChat(any(), any());
    }
}
