package com.binison.chatbot.service;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.llm.LlmClient;
import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.model.LlmResponse;
import com.binison.chatbot.util.MessageFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class ChatService {
    private final ChatBotPlugin plugin;
    private final SessionManager sessionManager;
    private final RateLimitService rateLimitService;
    private LlmClient llmClient;
    private final ExecutorService executorService;
    private PluginConfig config;

    public ChatService(ChatBotPlugin plugin, PluginConfig config, SessionManager sessionManager, RateLimitService rateLimitService, LlmClient llmClient) {
        this.plugin = plugin;
        this.config = config;
        this.sessionManager = sessionManager;
        this.rateLimitService = rateLimitService;
        this.llmClient = llmClient;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public void reload(PluginConfig newConfig) {
        this.config = newConfig;
        this.sessionManager.updateExpireAfter(newConfig.contextExpireMinutes());
    }

    public void setLlmClient(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void handleChat(Player player, String message) {
        if (!player.hasPermission("chatbot.use")) {
            player.sendMessage(noPermissionMessage());
            return;
        }
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            player.sendMessage(MessageFormatter.colorize(config.messages().empty()));
            return;
        }
        if (!config.enabled()) {
            player.sendMessage(MessageFormatter.colorize(config.messages().disabled()));
            return;
        }
        if (!config.apiConfigured()) {
            player.sendMessage(MessageFormatter.colorize(config.messages().missingApiKey()));
            return;
        }
        if (trimmed.length() > config.maxInputLength()) {
            player.sendMessage(MessageFormatter.colorize(config.messages().tooLong()));
            return;
        }
        if (config.rateLimitEnabled() && !player.hasPermission("chatbot.bypass.ratelimit") && rateLimitService.isCoolingDown(player.getUniqueId(), config.cooldownSeconds())) {
            player.sendMessage(MessageFormatter.colorize(config.messages().cooldown()));
            return;
        }
        if (!rateLimitService.tryAcquire(player.getUniqueId())) {
            player.sendMessage(MessageFormatter.colorize(config.messages().busy()));
            return;
        }

        player.sendMessage(MessageFormatter.colorize("&7Thinking..."));
        executorService.submit(() -> processRequest(player.getUniqueId(), player.getName(), trimmed));
    }

    private void processRequest(UUID playerId, String playerName, String message) {
        try {
            List<ChatMessage> context = sessionManager.getContext(playerId, config.contextEnabled(), config.contextMaxRounds(), config.systemPrompt());
            context.add(new ChatMessage("user", message));
            LlmResponse response = llmClient.chat(context);
            sessionManager.appendUser(playerId, message);
            sessionManager.appendAssistant(playerId, response.content());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    return;
                }
                for (String line : MessageFormatter.splitForChat(config.replyPrefix(), response.content(), config.maxOutputLength())) {
                    player.sendMessage(line);
                }
            });
        } catch (Exception exception) {
            plugin.getLogger().warning("AI request failed for " + playerName + ": " + exception.getMessage());
            if (config.debug()) {
                plugin.getLogger().log(Level.WARNING, "Detailed AI failure stack trace follows.", exception);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(MessageFormatter.colorize(config.messages().requestFailed()));
                }
            });
        } finally {
            rateLimitService.markCompleted(playerId);
        }
    }

    public void resetSession(UUID playerId) {
        sessionManager.reset(playerId);
        rateLimitService.clear(playerId);
    }

    public String noPermissionMessage() {
        return MessageFormatter.colorize(config.messages().noPermission());
    }

    public String resetDoneMessage() {
        return MessageFormatter.colorize(config.messages().resetDone());
    }

    public String reloadDoneMessage() {
        return MessageFormatter.colorize(config.messages().reloadDone());
    }

    public int maxPromptLength() {
        return config.maxPromptLength();
    }

    public String promptTooLongMessage() {
        return MessageFormatter.colorize(config.messages().promptTooLong());
    }

    public String promptViewMessage() {
        return MessageFormatter.colorize(String.format(config.messages().promptView(), config.systemPrompt()));
    }

    public String promptSetDoneMessage() {
        return MessageFormatter.colorize(config.messages().promptSetDone());
    }

    public String promptResetDoneMessage() {
        return MessageFormatter.colorize(config.messages().promptResetDone());
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
