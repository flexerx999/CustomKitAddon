package com.strikepractice.customkitaddon.commands;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CustomKitCommand implements Listener {

    private final CustomKitAddon plugin;
    private final Set<UUID> customKitUsers = new HashSet<>();

    public CustomKitCommand(CustomKitAddon plugin) {
        this.plugin = plugin;

        // Register as listener to intercept commands
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

        // Track when players use /customkit
        if (command.equals("/customkit") || command.startsWith("/customkit ")) {
            // Mark this player as using customkit
            customKitUsers.add(player.getUniqueId());

            // Remove after 10 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                customKitUsers.remove(player.getUniqueId());
            }, 200L);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " used /customkit command");
            }
        }

        // Check for /customkit items command
        if (command.startsWith("/customkit items")) {
            event.setCancelled(true);

            if (!player.hasPermission("customkit.use")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return;
            }

            // Parse page number
            String[] args = command.split(" ");
            int page = 1;

            if (args.length > 2) {
                try {
                    page = Math.max(1, Math.min(Integer.parseInt(args[2]), 7)); // Updated from 3 to 7
                } catch (NumberFormatException ignored) {
                }
            }

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Opening custom items GUI directly - Page: " + page);
            }

            // Open custom items GUI directly with default slot 0
            plugin.getGuiManager().openCustomItemsGUI(player, page, 0);
        }
    }

    public boolean isUsingCustomKit(Player player) {
        return customKitUsers.contains(player.getUniqueId());
    }

    public void removeUser(Player player) {
        customKitUsers.remove(player.getUniqueId());
    }
}