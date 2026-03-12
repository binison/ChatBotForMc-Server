package com.binison.chatbot.listener;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.service.ChatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ChatMentionListener implements Listener {
    private final ChatBotPlugin plugin;
    private final ChatService chatService;

    public ChatMentionListener(ChatBotPlugin plugin, ChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (config == null || !config.chatMention().enabled()) {
            return;
        }
        String plainMessage = plainText(event.message());
        String prompt = com.binison.chatbot.util.ChatMentionParser.extractPrompt(plainMessage, config.chatMention().prefixes());
        if (prompt == null) {
            return;
        }
        if (config.chatMention().cancelOriginalMessage()) {
            event.setCancelled(true);
        }
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> chatService.handleChat(player, prompt));
    }

    private String plainText(@NotNull net.kyori.adventure.text.Component component) {
        return component instanceof net.kyori.adventure.text.TextComponent textComponent
                ? textComponent.content()
                : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }
}
