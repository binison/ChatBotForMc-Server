package com.binison.chatbot.config;

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
        String replyPrefix,
        int maxInputLength,
        int maxOutputLength,
        boolean contextEnabled,
        int contextMaxRounds,
        long contextExpireMinutes,
        String systemPrompt,
        boolean rateLimitEnabled,
        long cooldownSeconds,
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
                Math.max(1, config.getInt("api.max-tokens", 300)),
                config.getDouble("api.temperature", 0.7d),
                config.getString("chat.reply-prefix", "&b[AI]&r "),
                Math.max(1, config.getInt("chat.max-input-length", 300)),
                Math.max(1, config.getInt("chat.max-output-length", 1200)),
                config.getBoolean("context.enabled", true),
                Math.max(1, config.getInt("context.max-rounds", 6)),
                Math.max(1, config.getLong("context.expire-minutes", 30L)),
                config.getString("context.system-prompt", "You are a helpful Minecraft server assistant."),
                config.getBoolean("rate-limit.enabled", true),
                Math.max(0, config.getLong("rate-limit.cooldown-seconds", 5L)),
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
                        config.getString("messages.reload-done", "&aChatbot configuration reloaded.")
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
            String reloadDone
    ) {}
}
