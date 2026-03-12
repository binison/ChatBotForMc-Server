package com.binison.chatbot;

import com.binison.chatbot.command.AiCommand;
import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.listener.ChatMentionListener;
import com.binison.chatbot.listener.EventTriggerListener;
import com.binison.chatbot.llm.HttpLlmClient;
import com.binison.chatbot.llm.LlmClient;
import com.binison.chatbot.service.ChatService;
import com.binison.chatbot.service.RateLimitService;
import com.binison.chatbot.service.SessionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatBotPlugin extends JavaPlugin {
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful Minecraft server assistant.";
    private PluginConfig pluginConfig;
    private SessionManager sessionManager;
    private RateLimitService rateLimitService;
    private LlmClient llmClient;
    private ChatService chatService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        this.sessionManager = new SessionManager(pluginConfig.contextExpireMinutes());
        this.rateLimitService = new RateLimitService();
        this.llmClient = new HttpLlmClient(pluginConfig);
        this.chatService = new ChatService(this, pluginConfig, sessionManager, rateLimitService, llmClient);

        PluginCommand aiCommand = getCommand("ai");
        if (aiCommand == null) {
            getLogger().severe("Command 'ai' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AiCommand executor = new AiCommand(this, chatService);
        aiCommand.setExecutor(executor);
        aiCommand.setTabCompleter(executor);
        getServer().getPluginManager().registerEvents(new EventTriggerListener(this, chatService), this);
        getServer().getPluginManager().registerEvents(new ChatMentionListener(this, chatService), this);

        getLogger().info("ChatBotForMc enabled.");
    }

    @Override
    public void onDisable() {
        if (chatService != null) {
            chatService.shutdown();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.pluginConfig = PluginConfig.from(getConfig());
        this.llmClient = new HttpLlmClient(pluginConfig);
        if (chatService != null) {
            chatService.reload(pluginConfig);
            chatService.setLlmClient(llmClient);
        }
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public ChatService chatService() {
        return chatService;
    }

    public void updateSystemPrompt(String prompt) {
        getConfig().set("context.system-prompt", prompt == null ? DEFAULT_SYSTEM_PROMPT : prompt.trim());
        saveConfig();
        reloadPluginConfig();
    }

    public void resetSystemPrompt() {
        getConfig().set("context.system-prompt", DEFAULT_SYSTEM_PROMPT);
        saveConfig();
        reloadPluginConfig();
    }
}
