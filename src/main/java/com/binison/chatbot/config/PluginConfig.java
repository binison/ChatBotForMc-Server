package com.binison.chatbot.config;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public record PluginConfig(
        boolean enabled,
        boolean debug,
        String provider,
        String apiBaseUrl,
        String apiKey,
        String authHeader,
        String authPrefix,
        String model,
        int timeoutMs,
        int maxTokens,
        double temperature,
        boolean streamEnabled,
        String replyPrefix,
        int maxInputLength,
        int maxOutputLength,
        int streamFlushChars,
        long streamFlushIntervalMs,
        ChatMention chatMention,
        boolean contextEnabled,
        int contextMaxRounds,
        long contextExpireMinutes,
        String systemPrompt,
        int maxPromptLength,
        boolean rateLimitEnabled,
        long cooldownSeconds,
        EventTriggers eventTriggers,
        CommandExecution commandExecution,
        Messages messages
) {
    public static PluginConfig from(FileConfiguration config) {
        String provider = normalizeProvider(config.getString("api.provider", "openai-compatible"));
        String defaultBaseUrl = defaultBaseUrl(provider);
        String defaultAuthHeader = defaultAuthHeader(provider);
        String defaultAuthPrefix = defaultAuthPrefix(provider);
        String defaultModel = defaultModel(provider);

        return new PluginConfig(
                config.getBoolean("enabled", true),
                config.getBoolean("debug", false),
                provider,
                config.getString("api.base-url", defaultBaseUrl),
                config.getString("api.api-key", ""),
                config.getString("api.auth-header", defaultAuthHeader),
                config.getString("api.auth-prefix", defaultAuthPrefix),
                config.getString("api.model", defaultModel),
                Math.max(1000, config.getInt("api.timeout-ms", 20000)),
                config.getInt("api.max-tokens", 300),
                config.getDouble("api.temperature", 0.7d),
                config.getBoolean("api.stream-enabled", true),
                config.getString("chat.reply-prefix", "&d[米糯]&r "),
                Math.max(1, config.getInt("chat.max-input-length", 300)),
                config.getInt("chat.max-output-length", 1200),
                Math.max(1, config.getInt("chat.stream-flush-chars", 24)),
                Math.max(50L, config.getLong("chat.stream-flush-interval-ms", 400L)),
                new ChatMention(
                        config.getBoolean("chat-mention.enabled", true),
                        config.getStringList("chat-mention.prefixes").isEmpty() ? List.of("@ai") : config.getStringList("chat-mention.prefixes"),
                        config.getBoolean("chat-mention.cancel-original-message", true)
                ),
                config.getBoolean("context.enabled", true),
                Math.max(1, config.getInt("context.max-rounds", 6)),
                Math.max(1, config.getLong("context.expire-minutes", 30L)),
                config.getString("context.system-prompt", "You are a helpful Minecraft server assistant."),
                Math.max(1, config.getInt("context.max-prompt-length", 2000)),
                config.getBoolean("rate-limit.enabled", true),
                Math.max(0, config.getLong("rate-limit.cooldown-seconds", 5L)),
                new EventTriggers(
                        config.getBoolean("event-triggers.enabled", false),
                        config.getBoolean("event-triggers.broadcast", true),
                        new EventTrigger(
                                config.getBoolean("event-triggers.join.enabled", false),
                                config.getBoolean("event-triggers.join.first-join-only", false),
                                Math.max(0L, config.getLong("event-triggers.join.cooldown-seconds", 300L)),
                                config.getString("event-triggers.join.prompt", "A player named %player% has joined the server. Give a short in-character welcome and one helpful server tip.")
                        ),
                        new EventTrigger(
                                config.getBoolean("event-triggers.death.enabled", false),
                                false,
                                Math.max(0L, config.getLong("event-triggers.death.cooldown-seconds", 180L)),
                                config.getString("event-triggers.death.prompt", "Player %player% died in Minecraft. Offer a short comforting reaction and one practical suggestion based on this death message: %death_message%")
                        ),
                        new AdvancementTrigger(
                                config.getBoolean("event-triggers.advancement.enabled", false),
                                false,
                                Math.max(0L, config.getLong("event-triggers.advancement.cooldown-seconds", 180L)),
                                config.getString("event-triggers.advancement.prompt", "Player %player% has just completed the advancement '%advancement%'. React briefly and suggest a natural next goal."),
                                config.getStringList("event-triggers.advancement.ignore-prefixes")
                        )
                ),
                CommandExecution.from(config.getConfigurationSection("command-execution")),
                new Messages(
                        config.getString("messages.disabled", "&cChatbot is currently disabled."),
                        config.getString("messages.missing-api-key", "&cChatbot API key is not configured."),
                        config.getString("messages.no-permission", "&cYou do not have permission to use this command."),
                        config.getString("messages.too-long", "&cYour message is too long."),
                        config.getString("messages.cooldown", "&ePlease wait a moment before sending another request."),
                        config.getString("messages.empty", "&ePlease enter a message."),
                        config.getString("messages.busy", "&eYour previous AI request is still running."),
                        config.getString("messages.request-failed", "&cAI is currently unavailable. Please try again later."),
                        config.getString("messages.reset-done", "&aConversation context cleared."),
                        config.getString("messages.reload-done", "&aChatbot configuration reloaded."),
                        config.getString("messages.prompt-too-long", "&cThe prompt is too long."),
                        config.getString("messages.prompt-view", "&7Current system prompt: &f%s"),
                        config.getString("messages.prompt-set-done", "&aSystem prompt updated and saved."),
                        config.getString("messages.prompt-reset-done", "&aSystem prompt reset and saved.")
                )
        );
    }

    public boolean apiConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && apiBaseUrl != null && !apiBaseUrl.isBlank()
                && authHeader != null && !authHeader.isBlank()
                && model != null && !model.isBlank();
    }

    public String authorizationValue() {
        if (authPrefix == null || authPrefix.isBlank()) {
            return apiKey == null ? "" : apiKey;
        }
        return authPrefix + apiKey;
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "openai-compatible";
        }
        return provider.trim().toLowerCase();
    }

    private static String defaultBaseUrl(String provider) {
        return switch (provider) {
            case "dashscope", "aliyun-bailian", "bailian", "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            default -> "https://api.openai.com/v1/chat/completions";
        };
    }

    private static String defaultAuthHeader(String provider) {
        return "Authorization";
    }

    private static String defaultAuthPrefix(String provider) {
        return "Bearer ";
    }

    private static String defaultModel(String provider) {
        return switch (provider) {
            case "dashscope", "aliyun-bailian", "bailian", "qwen" -> "qwen-plus";
            default -> "gpt-4o-mini";
        };
    }

    public record EventTriggers(
            boolean enabled,
            boolean broadcast,
            EventTrigger join,
            EventTrigger death,
            AdvancementTrigger advancement
    ) {
        public static EventTriggers from(org.bukkit.configuration.ConfigurationSection section) {
            if (section == null) {
                return new EventTriggers(false, false, null, null, null);
            }
            return new EventTriggers(
                    section.getBoolean("enabled", false),
                    section.getBoolean("broadcast", true),
                    EventTrigger.from(section.getConfigurationSection("join")),
                    EventTrigger.from(section.getConfigurationSection("death")),
                    AdvancementTrigger.from(section.getConfigurationSection("advancement"))
            );
        }
    }

    public record EventTrigger(
            boolean enabled,
            boolean firstJoinOnly,
            long cooldownSeconds,
            String prompt
    ) {
        public static EventTrigger from(org.bukkit.configuration.ConfigurationSection section) {
            if (section == null) {
                return new EventTrigger(false, false, 0L, "");
            }
            return new EventTrigger(
                    section.getBoolean("enabled", false),
                    section.getBoolean("first-join-only", false),
                    Math.max(0L, section.getLong("cooldown-seconds", 0L)),
                    section.getString("prompt", "")
            );
        }
    }

    public record AdvancementTrigger(
            boolean enabled,
            boolean firstJoinOnly,
            long cooldownSeconds,
            String prompt,
            List<String> ignorePrefixes
    ) {
        public static AdvancementTrigger from(org.bukkit.configuration.ConfigurationSection section) {
            if (section == null) {
                return new AdvancementTrigger(false, false, 0L, "", List.of());
            }
            return new AdvancementTrigger(
                    section.getBoolean("enabled", false),
                    section.getBoolean("first-join-only", false),
                    Math.max(0L, section.getLong("cooldown-seconds", 0L)),
                    section.getString("prompt", ""),
                    section.getStringList("ignore-prefixes")
            );
        }

        public boolean shouldIgnore(String key) {
            if (key == null || key.isBlank()) {
                return false;
            }
            if (ignorePrefixes == null || ignorePrefixes.isEmpty()) {
                return false;
            }
            return ignorePrefixes.stream()
                    .filter(prefix -> prefix != null && !prefix.isBlank())
                    .map(String::trim)
                    .anyMatch(key::startsWith);
        }
    }

    public record Messages(
            String disabled,
            String missingApiKey,
            String noPermission,
            String tooLong,
            String cooldown,
            String empty,
            String busy,
            String requestFailed,
            String resetDone,
            String reloadDone,
            String promptTooLong,
            String promptView,
            String promptSetDone,
            String promptResetDone
    ) {
        public static Messages from(org.bukkit.configuration.ConfigurationSection section) {
            if (section == null) {
                return new Messages(
                        "&cChatbot is currently disabled.",
                        "&cChatbot API key is not configured.",
                        "&cYou do not have permission to use this command.",
                        "&cYour message is too long.",
                        "&ePlease wait a moment before sending another request.",
                        "&ePlease enter a message.",
                        "&eYour previous AI request is still running.",
                        "&cAI is currently unavailable. Please try again later.",
                        "&aConversation context cleared.",
                        "&aChatbot configuration reloaded.",
                        "&cThe prompt is too long.",
                        "&7Current system prompt: &f%s",
                        "&aSystem prompt updated and saved.",
                        "&aSystem prompt reset and saved."
                );
            }
            return new Messages(
                    section.getString("disabled", "&cChatbot is currently disabled."),
                    section.getString("missing-api-key", "&cChatbot API key is not configured."),
                    section.getString("no-permission", "&cYou do not have permission to use this command."),
                    section.getString("too-long", "&cYour message is too long."),
                    section.getString("cooldown", "&ePlease wait a moment before sending another request."),
                    section.getString("empty", "&ePlease enter a message."),
                    section.getString("busy", "&eYour previous AI request is still running."),
                    section.getString("request-failed", "&cAI is currently unavailable. Please try again later."),
                    section.getString("reset-done", "&aConversation context cleared."),
                    section.getString("reload-done", "&aChatbot configuration reloaded."),
                    section.getString("prompt-too-long", "&cThe prompt is too long."),
                    section.getString("prompt-view", "&7Current system prompt: &f%s"),
                    section.getString("prompt-set-done", "&aSystem prompt updated and saved."),
                    section.getString("prompt-reset-done", "&aSystem prompt reset and saved.")
            );
        }
    }

    public record ChatMention(
            boolean enabled,
            List<String> prefixes,
            boolean cancelOriginalMessage
    ) {}

    public record CommandExecution(
            boolean enabled,
            boolean requireConfirm,
            int maxCommandsPerResponse,
            int maxCommandLength,
            List<String> allowedCommands
    ) {
        public static CommandExecution from(org.bukkit.configuration.ConfigurationSection section) {
            if (section == null) {
                return new CommandExecution(false, true, 1, 200, List.of());
            }
            return new CommandExecution(
                    section.getBoolean("enabled", false),
                    section.getBoolean("require-confirm", true),
                    Math.max(1, section.getInt("max-commands-per-response", 1)),
                    Math.max(1, section.getInt("max-command-length", 200)),
                    section.getStringList("allowed-commands")
            );
        }
    }
}
