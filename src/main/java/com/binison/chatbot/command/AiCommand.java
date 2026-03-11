package com.binison.chatbot.command;

import com.binison.chatbot.ChatBotPlugin;
import com.binison.chatbot.service.ChatService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AiCommand implements CommandExecutor, TabCompleter {
    private final ChatBotPlugin plugin;
    private final ChatService chatService;

    public AiCommand(ChatBotPlugin plugin, ChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/ai <message>");
            sender.sendMessage("/ai reload");
            sender.sendMessage("/ai reset [player]");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            if (!sender.hasPermission("chatbot.admin")) {
                sender.sendMessage(chatService.noPermissionMessage());
                return true;
            }
            plugin.reloadPluginConfig();
            chatService.reload(plugin.pluginConfig());
            sender.sendMessage(chatService.reloadDoneMessage());
            return true;
        }

        if (sub.equals("reset")) {
            if (!sender.hasPermission("chatbot.admin")) {
                sender.sendMessage(chatService.noPermissionMessage());
                return true;
            }
            if (args.length >= 2) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target != null) {
                    chatService.resetSession(target.getUniqueId());
                    sender.sendMessage("Reset session for " + target.getName());
                } else {
                    sender.sendMessage("Player not found.");
                }
                return true;
            }
            if (sender instanceof Player player) {
                chatService.resetSession(player.getUniqueId());
                sender.sendMessage(chatService.resetDoneMessage());
            } else {
                sender.sendMessage("Console must specify a player.");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot start chat sessions. Use /ai reset <player> or /ai reload.");
            return true;
        }

        String message = String.join(" ", args);
        chatService.handleChat(player, message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) suggestions.add("reload");
            if ("reset".startsWith(args[0].toLowerCase())) suggestions.add("reset");
        }
        return suggestions;
    }
}

