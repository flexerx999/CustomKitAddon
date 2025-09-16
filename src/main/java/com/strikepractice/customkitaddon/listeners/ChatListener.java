package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final CustomKitAddon plugin;
    private final Map<UUID, RenameSession> renameSessions = new HashMap<>();

    public ChatListener(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    public void startRename(Player player) {
        if (!player.hasPermission("customkit.rename")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        // Cancel any existing session
        cancelRename(player);

        // Create new session with timeout
        RenameSession session = new RenameSession();
        session.task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (renameSessions.remove(player.getUniqueId()) != null) {
                player.sendMessage(plugin.getConfigManager().getMessage("rename-timeout"));
                // Reopen customkit GUI
                player.performCommand("customkit");
            }
        }, plugin.getConfigManager().getRenameTimeout() * 20L);

        renameSessions.put(player.getUniqueId(), session);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!renameSessions.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage();

        // Handle cancel
        if (message.equalsIgnoreCase("cancel")) {
            cancelRename(player);
            player.sendMessage(plugin.getConfigManager().getMessage("rename-cancelled"));

            // Reopen GUI
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand("customkit");
            });
            return;
        }

        // Process the new name
        String newName = message.replace("&", "§");

        // Validate name length
        if (newName.length() > 32) {
            player.sendMessage(plugin.getConfigManager().getMessage("rename-too-long"));
            return;
        }

        // Apply the new name
        Bukkit.getScheduler().runTask(plugin, () -> {
            applyKitName(player, newName);
        });

        // Cancel the session
        cancelRename(player);

        player.sendMessage(plugin.getConfigManager().getMessage("rename-success")
                .replace("%name%", newName));

        // Reopen customkit GUI
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.performCommand("customkit");
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelRename(event.getPlayer());
    }

    private void applyKitName(Player player, String name) {
        try {
            // Since StrikePractice API doesn't support kit renaming,
            // we'll store the custom names in our own config

            // Save the custom name in our config
            String path = "custom-kit-names." + player.getUniqueId().toString();
            plugin.getConfigManager().getConfig().set(path, name);
            plugin.getConfigManager().save();

            // Notify the player
            player.sendMessage(plugin.getConfigManager().getMessage("rename-success")
                    .replace("%name%", name));

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Saved custom kit name '" + name + "' for " + player.getName());
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save kit name: " + e.getMessage());
            player.sendMessage(plugin.getConfigManager().getMessage("rename-error"));
        }
    }

    public void cancelRename(Player player) {
        RenameSession session = renameSessions.remove(player.getUniqueId());
        if (session != null && session.task != null) {
            session.task.cancel();
        }
    }

    public void cleanup() {
        for (RenameSession session : renameSessions.values()) {
            if (session.task != null) {
                session.task.cancel();
            }
        }
        renameSessions.clear();
    }

    private static class RenameSession {
        BukkitTask task;
    }
}