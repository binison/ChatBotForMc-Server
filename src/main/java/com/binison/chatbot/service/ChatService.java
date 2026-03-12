package com.binison.chatbot.service;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.llm.LlmClient;
import com.binison.chatbot.llm.StreamingChunkListener;
import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.util.MessageFormatter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class ChatService {
    private final ChatBotPlugin plugin;
    private final SessionManager sessionManager;
    private final RateLimitService rateLimitService;
    private final Map<String, Instant> eventCooldowns = new ConcurrentHashMap<>();
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
        this.eventCooldowns.clear();
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
        submitPlayerRequest(player, trimmed, true, ResponseVisibility.PRIVATE);
    }

    public void handleTriggeredEvent(Player player, String eventKey, String prompt, long cooldownSeconds, boolean broadcast) {
        if (player == null || prompt == null) {
            return;
        }
        String trimmed = prompt.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (isEventCoolingDown(player.getUniqueId(), eventKey, cooldownSeconds)) {
            return;
        }
        ResponseVisibility visibility = broadcast ? ResponseVisibility.BROADCAST : ResponseVisibility.PRIVATE;
        if (submitPlayerRequest(player, trimmed, false, visibility)) {
            markEventTriggered(player.getUniqueId(), eventKey);
        }
    }

    private boolean submitPlayerRequest(Player player, String message, boolean showThinkingMessage, ResponseVisibility visibility) {
        if (!config.enabled()) {
            if (showThinkingMessage) {
                player.sendMessage(MessageFormatter.colorize(config.messages().disabled()));
            }
            return false;
        }
        if (!config.apiConfigured()) {
            if (showThinkingMessage) {
                player.sendMessage(MessageFormatter.colorize(config.messages().missingApiKey()));
            }
            return false;
        }
        if (message.length() > config.maxInputLength()) {
            if (showThinkingMessage) {
                player.sendMessage(MessageFormatter.colorize(config.messages().tooLong()));
            }
            return false;
        }
        if (!rateLimitService.tryAcquire(player.getUniqueId())) {
            if (showThinkingMessage) {
                player.sendMessage(MessageFormatter.colorize(config.messages().busy()));
            }
            return false;
        }

        if (showThinkingMessage) {
            player.sendMessage(MessageFormatter.colorize("&7Thinking..."));
        }
        executorService.submit(() -> processRequest(player.getUniqueId(), player.getName(), message, visibility));
        return true;
    }

    private boolean isEventCoolingDown(UUID playerId, String eventKey, long cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return false;
        }
        Instant lastTriggered = eventCooldowns.get(eventCooldownKey(playerId, eventKey));
        return lastTriggered != null && lastTriggered.plusSeconds(cooldownSeconds).isAfter(Instant.now());
    }

    private void markEventTriggered(UUID playerId, String eventKey) {
        eventCooldowns.put(eventCooldownKey(playerId, eventKey), Instant.now());
    }

    private String eventCooldownKey(UUID playerId, String eventKey) {
        return playerId + ":" + eventKey;
    }

    private void processRequest(UUID playerId, String playerName, String message, ResponseVisibility visibility) {
        StringBuilder fullResponse = new StringBuilder();
        StringBuilder pending = new StringBuilder();
        long[] lastFlushAt = {System.currentTimeMillis()};
        try {
            List<ChatMessage> context = sessionManager.getContext(playerId, config.contextEnabled(), config.contextMaxRounds(), config.systemPrompt());
            context.add(new ChatMessage("user", message));

            llmClient.streamChat(context, text -> {
                if (text == null || text.isEmpty()) {
                    return;
                }
                fullResponse.append(text);
                pending.append(text);
                long now = System.currentTimeMillis();
                boolean shouldFlushBySize = pending.length() >= config.streamFlushChars();
                boolean shouldFlushByTime = now - lastFlushAt[0] >= config.streamFlushIntervalMs();
                if (shouldFlushBySize || shouldFlushByTime || endsAtNaturalBoundary(text)) {
                    flushPending(playerId, pending, false, visibility);
                    lastFlushAt[0] = now;
                }
            });

            while (!pending.isEmpty()) {
                flushPending(playerId, pending, true, visibility);
            }
            String assistantReply = MessageFormatter.sanitizeContent(fullResponse.toString()).trim();
            if (!assistantReply.isEmpty()) {
                sessionManager.appendUser(playerId, message);
                sessionManager.appendAssistant(playerId, assistantReply);
            }
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

    private void flushPending(UUID playerId, StringBuilder pending, boolean force, ResponseVisibility visibility) {
        List<String> lines = MessageFormatter.consumeStreamableSegments(
                config.replyPrefix(),
                pending,
                config.maxOutputLength(),
                config.streamFlushChars(),
                force
        );
        if (lines.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            if (visibility == ResponseVisibility.BROADCAST) {
                for (String line : lines) {
                    plugin.getServer().broadcastMessage(line);
                }
                return;
            }
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }
            for (String line : lines) {
                player.sendMessage(line);
            }
        });
    }

    private boolean endsAtNaturalBoundary(String text) {
        char last = text.charAt(text.length() - 1);
        return Character.isWhitespace(last) || "。！？!?；;，,、.\n".indexOf(last) >= 0;
    }

    public void resetSession(UUID playerId) {
        sessionManager.reset(playerId);
        rateLimitService.clear(playerId);
        eventCooldowns.keySet().removeIf(key -> key.startsWith(playerId + ":"));
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

    private enum ResponseVisibility {
        PRIVATE,
        BROADCAST
    }
}
