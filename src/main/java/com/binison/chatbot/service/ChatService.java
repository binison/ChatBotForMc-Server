package com.binison.chatbot.service;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.llm.LlmClient;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class ChatService {
    private static final Pattern CMD_PATTERN = Pattern.compile("\\{\\{EXECUTE:([^\\r\\n}]{1,200})}}", Pattern.MULTILINE);
    private static final int COMMAND_TAG_PREFIX_LEN = "{{EXECUTE:".length();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final long CONFIRM_TTL_MS = 60_000L;

    private final ChatBotPlugin plugin;
    private final SessionManager sessionManager;
    private final RateLimitService rateLimitService;
    private final Map<String, Instant> eventCooldowns = new ConcurrentHashMap<>();
    private volatile LlmClient llmClient;
    private final ExecutorService executorService;
    private volatile PluginConfig config;

    private final Map<String, PendingCommand> pendingCommands = new ConcurrentHashMap<>();

    // Reply target.
    private enum ResponseVisibility {
        PRIVATE,
        BROADCAST
    }

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
        // Pending confirmations are tied to old responses/config; drop them on reload.
        this.pendingCommands.clear();
    }

    /**
     * Stop background workers and release transient state. Call from plugin onDisable().
     */
    public void shutdown() {
        try {
            executorService.shutdownNow();
        } catch (Exception ignored) {
            // Best-effort shutdown.
        }
        pendingCommands.clear();
        eventCooldowns.clear();
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
        submitPlayerRequest(player, trimmed, true, ResponseVisibility.PRIVATE, true);
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
        if (submitPlayerRequest(player, trimmed, false, visibility, false)) {
            markEventTriggered(player.getUniqueId(), eventKey);
        }
    }

    private boolean submitPlayerRequest(Player player, String message, boolean showThinkingMessage, ResponseVisibility visibility, boolean allowCommandExecution) {
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
            player.sendMessage(MessageFormatter.colorize("&d[米糯]&r emmm~"));
        }
        executorService.submit(() -> processRequest(player.getUniqueId(), player.getName(), message, visibility, allowCommandExecution));
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

    private void processRequest(UUID playerId, String playerName, String message, ResponseVisibility visibility, boolean allowCommandExecution) {
        // Snapshot mutable shared references for thread-safety during reloads.
        final PluginConfig cfg = this.config;
        final LlmClient client = this.llmClient;

        StringBuilder fullResponse = new StringBuilder();
        StringBuilder pending = new StringBuilder();
        StringBuilder commandBuffer = new StringBuilder();

        boolean allowCommandExecutionLocal = allowCommandExecution && cfg.commandExecution().enabled() && visibility == ResponseVisibility.PRIVATE;

        long[] lastFlushAt = {System.currentTimeMillis()};
        try {
            String systemPrompt = cfg.systemPrompt();
            if (allowCommandExecutionLocal) {
                String allowed = String.join(", ", cfg.commandExecution().allowedCommands());
                systemPrompt += "\n\nIf the user asks you to perform an action available via command, you MUST append {{EXECUTE:/command}} to the end of your response. Supported commands: " + allowed;
            }

            List<ChatMessage> context = sessionManager.getContext(playerId, cfg.contextEnabled(), cfg.contextMaxRounds(), systemPrompt);
            context.add(new ChatMessage("user", message));
            sessionManager.appendUser(playerId, message);

            client.streamChat(context, text -> {
                if (text == null || text.isEmpty()) {
                    return;
                }
                fullResponse.append(text);

                if (allowCommandExecutionLocal) {
                    commandBuffer.append(text);
                    while (true) {
                        String data = commandBuffer.toString();
                        int tagStart = data.indexOf("{{EXECUTE:");

                        if (tagStart == -1) {
                            String prefix = "{{EXECUTE:";
                            int maxPartialLen = Math.min(data.length(), prefix.length() - 1);
                            int unsafeLen = 0;
                            for (int len = 1; len <= maxPartialLen; len++) {
                                if (data.regionMatches(data.length() - len, prefix, 0, len)) {
                                    unsafeLen = len;
                                }
                            }

                            if (unsafeLen > 0) {
                                int flushLen = data.length() - unsafeLen;
                                if (flushLen > 0) {
                                    pending.append(commandBuffer, 0, flushLen);
                                    commandBuffer.delete(0, flushLen);
                                }
                                break;
                            } else {
                                pending.append(commandBuffer);
                                commandBuffer.setLength(0);
                                break;
                            }
                        } else if (tagStart > 0) {
                            pending.append(commandBuffer, 0, tagStart);
                            commandBuffer.delete(0, tagStart);
                        } else {
                            int tagEnd = data.indexOf("}}", COMMAND_TAG_PREFIX_LEN);
                            if (tagEnd != -1) {
                                commandBuffer.delete(0, tagEnd + 2);
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    pending.append(text);
                }

                long now = System.currentTimeMillis();
                boolean shouldFlushBySize = pending.length() >= cfg.streamFlushChars();
                boolean shouldFlushByTime = now - lastFlushAt[0] >= cfg.streamFlushIntervalMs();
                if (shouldFlushBySize || shouldFlushByTime || endsAtNaturalBoundary(text)) {
                    flushPending(playerId, pending, false, visibility);
                    lastFlushAt[0] = now;
                }
            });

            if (allowCommandExecutionLocal && !commandBuffer.isEmpty()) {
                pending.append(commandBuffer);
            }

            while (!pending.isEmpty()) {
                flushPending(playerId, pending, true, visibility);
            }

            String rawResponse = fullResponse.toString();
            String sanitizedResponse = stripCommandTags(rawResponse);
            sessionManager.appendAssistant(playerId, sanitizedResponse);

            if (allowCommandExecutionLocal) {
                plugin.getServer().getScheduler().runTask(plugin, () -> handleCommands(playerId, rawResponse));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("AI request failed for " + playerName + ": " + e.getMessage());
            if (cfg.debug()) {
                plugin.getLogger().log(Level.WARNING, "Detailed AI failure stack trace follows.", e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(MessageFormatter.colorize(cfg.messages().requestFailed()));
                }
            });
        } finally {
            rateLimitService.markCompleted(playerId);
        }
    }

    private void flushPending(UUID playerId, StringBuilder pending, boolean force, ResponseVisibility visibility) {
        final PluginConfig cfg = this.config;
        List<String> lines = MessageFormatter.consumeStreamableSegments(
                cfg.replyPrefix(),
                pending,
                cfg.maxOutputLength(),
                cfg.streamFlushChars(),
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
                    plugin.getServer().sendMessage(LEGACY.deserialize(line));
                }
                return;
            }
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }
            for (String line : lines) {
                player.sendMessage(LEGACY.deserialize(line));
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

    public boolean confirmCommand(Player player, String token) {
        // opportunistic cleanup
        cleanupExpiredPendingCommands();
        if (player == null || token == null || token.isBlank()) {
            return false;
        }
        PendingCommand pending = pendingCommands.remove(token);
        if (pending == null) {
            return false;
        }
        if (!pending.playerId().equals(player.getUniqueId())) {
            return false;
        }
        if (System.currentTimeMillis() - pending.createdAtMs() > CONFIRM_TTL_MS) {
            return false;
        }
        player.performCommand(pending.command());
        // No user-facing message: avoid exposing execution details.
        return true;
    }

    private void handleCommands(UUID playerId, String fullResponse) {
        cleanupExpiredPendingCommands();
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        Matcher matcher = CMD_PATTERN.matcher(fullResponse);
        int executed = 0;
        while (matcher.find()) {
            if (executed >= config.commandExecution().maxCommandsPerResponse()) {
                break;
            }
            String command = matcher.group(1).trim();
            if (command.startsWith("/")) command = command.substring(1);
            if (command.isBlank() || command.length() > config.commandExecution().maxCommandLength()) {
                continue;
            }

            if (isCommandAllowed(command)) {
                executed++;
                if (config.commandExecution().requireConfirm()) {
                    String token = createConfirmToken(playerId, command);
                    Component runBtn = Component.text("[确定]")
                            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.runCommand("/ai confirm " + token))
                            .hoverEvent(HoverEvent.showText(Component.text("点击确认")));
                    player.sendMessage(runBtn);
                } else {
                    player.performCommand(command);
                    // No user-facing message.
                }
            } else {
                plugin.getLogger().warning("AI attempted to run disallowed command: " + command);
                String allowedList = String.join(", ", config.commandExecution().allowedCommands());
                player.sendMessage(LEGACY.deserialize("&cAI 请求了未允许的指令 &7(仅允许: " + allowedList + ")"));
            }
        }
    }

    private String createConfirmToken(UUID playerId, String command) {
        String token = Long.toHexString(ThreadLocalRandom.current().nextLong())
                + Long.toHexString(System.nanoTime());
        pendingCommands.put(token, new PendingCommand(playerId, command, System.currentTimeMillis()));
        return token;
    }

    private void cleanupExpiredPendingCommands() {
        long now = System.currentTimeMillis();
        pendingCommands.entrySet().removeIf(e -> now - e.getValue().createdAtMs() > CONFIRM_TTL_MS);
    }

    private record PendingCommand(UUID playerId, String command, long createdAtMs) {}

    private boolean isCommandAllowed(String command) {
        String normalizedCmd = normalizeCommand(command);
        for (String allowed : config.commandExecution().allowedCommands()) {
            String normalizedAllowed = normalizeCommand(allowed);
            if (normalizedAllowed.endsWith("*")) {
                String prefix = normalizedAllowed.substring(0, normalizedAllowed.length() - 1);
                if (!prefix.isEmpty() && normalizedCmd.startsWith(prefix)) {
                    return true;
                }
                continue;
            }
            if (normalizedCmd.equals(normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.toLowerCase().replaceAll("\\s+", " ");
    }

    private String stripCommandTags(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return "";
        }
        // Remove all {{EXECUTE:...}} blocks.
        return CMD_PATTERN.matcher(rawResponse).replaceAll("").trim();
    }
}
