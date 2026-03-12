package com.binison.chatbot.listener;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.service.ChatService;
import org.bukkit.Location;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class EventTriggerListener implements Listener {
    private final ChatBotPlugin plugin;
    private final ChatService chatService;

    public EventTriggerListener(ChatBotPlugin plugin, ChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.eventTriggers().enabled() || !config.eventTriggers().join().enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (config.eventTriggers().join().firstJoinOnly() && player.hasPlayedBefore()) {
            return;
        }
        String prompt = applyTemplate(config.eventTriggers().join().prompt(), player)
                .replace("%death_message%", "")
                .replace("%death_x%", "")
                .replace("%death_y%", "")
                .replace("%death_z%", "")
                .replace("%death_world%", "")
                .replace("%death_location%", "")
                .replace("%advancement%", "");
        chatService.handleTriggeredEvent(player, "join", prompt, config.eventTriggers().join().cooldownSeconds(), config.eventTriggers().broadcast());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.eventTriggers().enabled() || !config.eventTriggers().death().enabled()) {
            return;
        }
        Player player = event.getPlayer();
        String deathMessage = event.getDeathMessage() == null ? "unknown" : event.getDeathMessage();
        Location location = player.getLocation();
        String deathX = String.valueOf(location.getBlockX());
        String deathY = String.valueOf(location.getBlockY());
        String deathZ = String.valueOf(location.getBlockZ());
        String deathWorld = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        String deathLocation = deathWorld + " (" + deathX + ", " + deathY + ", " + deathZ + ")";
        String prompt = applyTemplate(config.eventTriggers().death().prompt(), player)
                .replace("%death_message%", deathMessage)
                .replace("%death_x%", deathX)
                .replace("%death_y%", deathY)
                .replace("%death_z%", deathZ)
                .replace("%death_world%", deathWorld)
                .replace("%death_location%", deathLocation)
                .replace("%advancement%", "");
        chatService.handleTriggeredEvent(player, "death", prompt, config.eventTriggers().death().cooldownSeconds(), config.eventTriggers().broadcast());
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.eventTriggers().enabled() || !config.eventTriggers().advancement().enabled()) {
            return;
        }
        Advancement advancement = event.getAdvancement();
        String key = advancement.getKey().toString();
        if (config.eventTriggers().advancement().shouldIgnore(key)) {
            return;
        }
        String prompt = applyTemplate(config.eventTriggers().advancement().prompt(), event.getPlayer())
                .replace("%death_message%", "")
                .replace("%death_x%", "")
                .replace("%death_y%", "")
                .replace("%death_z%", "")
                .replace("%death_world%", "")
                .replace("%death_location%", "")
                .replace("%advancement%", key);
        chatService.handleTriggeredEvent(event.getPlayer(), "advancement", prompt, config.eventTriggers().advancement().cooldownSeconds(), config.eventTriggers().broadcast());
    }

    private String applyTemplate(String template, Player player) {
        String safeTemplate = template == null ? "" : template;
        return safeTemplate.replace("%player%", player.getName());
    }
}
