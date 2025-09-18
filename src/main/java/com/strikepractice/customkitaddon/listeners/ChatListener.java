package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.playerkits.PlayerKits;
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
        // Cancel any existing session
        cancelRename(player);

        // Force close any open inventory first
        player.closeInventory();

        // Create new session with timeout
        RenameSession session = new RenameSession();
        session.task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (renameSessions.remove(player.getUniqueId()) != null) {
                player.sendMessage(plugin.getConfigManager().getMessage("rename-timeout"));

                // Reopen customkit GUI
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.performCommand("customkit");
                });
            }
        }, plugin.getConfigManager().getRenameTimeout() * 20L);

        renameSessions.put(player.getUniqueId(), session);

        // Send prompt message immediately
        player.sendMessage(plugin.getConfigManager().getMessage("rename-prompt"));
        // Remove the cancel message as requested
        // player.sendMessage(plugin.getConfigManager().getMessage("rename-cancel-info"));

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Started rename session for " + player.getName());
        }
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

        // Process the new name (support color codes)
        String newName = message.replace("&", "§");

        // Validate name length
        if (newName.length() > 32) {
            player.sendMessage(plugin.getConfigManager().getMessage("rename-too-long"));
            return;
        }

        // Cancel the session first
        cancelRename(player);

        // Apply the new name on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = applyKitName(player, newName);

            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("rename-success")
                        .replace("%name%", newName));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("rename-error"));
            }

            // Reopen customkit GUI after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.performCommand("customkit");
            }, 5L); // Increased delay to ensure proper GUI opening
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelRename(event.getPlayer());
    }

    private boolean applyKitName(Player player, String name) {
        try {
            StrikePracticeAPI api = plugin.getStrikePracticeAPI();
            PlayerKits playerKits = api.getPlayerKits(player);

            // Get the current custom kit
            BattleKit customKit = playerKits.getCustomKit();

            if (customKit == null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("No custom kit found for renaming");
                }
                return false;
            }

            // Try multiple approaches to set the name
            boolean success = false;

            // Approach 1: Try setName method first (most reliable)
            try {
                java.lang.reflect.Method setName = customKit.getClass().getMethod("setName", String.class);
                setName.invoke(customKit, name);
                success = true;
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Set kit name using setName method");
                }
            } catch (NoSuchMethodException e1) {
                // Try setDisplayName method
                try {
                    java.lang.reflect.Method setDisplayName = customKit.getClass().getMethod("setDisplayName", String.class);
                    setDisplayName.invoke(customKit, name);
                    success = true;
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Set kit name using setDisplayName method");
                    }
                } catch (NoSuchMethodException e2) {
                    // Try direct field access as last resort
                    try {
                        java.lang.reflect.Field nameField = customKit.getClass().getDeclaredField("name");
                        nameField.setAccessible(true);
                        nameField.set(customKit, name);
                        success = true;
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Set kit name using name field");
                        }
                    } catch (NoSuchFieldException e3) {
                        try {
                            java.lang.reflect.Field nameField = customKit.getClass().getDeclaredField("displayName");
                            nameField.setAccessible(true);
                            nameField.set(customKit, name);
                            success = true;
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("Set kit name using displayName field");
                            }
                        } catch (NoSuchFieldException e4) {
                            // Try to find any field containing "name"
                            for (java.lang.reflect.Field field : customKit.getClass().getDeclaredFields()) {
                                if (field.getName().toLowerCase().contains("name") &&
                                        field.getType() == String.class) {
                                    field.setAccessible(true);
                                    field.set(customKit, name);
                                    success = true;
                                    if (plugin.getConfigManager().isDebugEnabled()) {
                                        plugin.getLogger().info("Set kit name using field: " + field.getName());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // After setting the name, save the kit
            if (success) {
                // Use the savePlayerKitsToFile method directly
                playerKits.savePlayerKitsToFile();

                // Try additional save methods if available
                try {
                    java.lang.reflect.Method saveMethod = playerKits.getClass().getMethod("save");
                    saveMethod.invoke(playerKits);
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Saved kit using save method");
                    }
                } catch (Exception e) {
                    // Ignore if not available
                }

                // Try to call any update methods on the API
                try {
                    java.lang.reflect.Method updateMethod = api.getClass().getMethod("updatePlayerKits", Player.class);
                    updateMethod.invoke(api, player);
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Updated player kits using API");
                    }
                } catch (Exception e) {
                    // Ignore if not available
                }

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Saved kit using savePlayerKitsToFile method");
                }
            }

            return success;
        } catch (Exception e) {
            plugin.getLogger().warning("Error renaming kit: " + e.getMessage());
            e.printStackTrace();
            return false;
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