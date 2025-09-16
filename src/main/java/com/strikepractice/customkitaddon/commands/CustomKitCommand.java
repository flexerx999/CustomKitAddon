package com.strikepractice.customkitaddon.commands;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CustomKitCommand implements Listener {

    private final CustomKitAddon plugin;

    public CustomKitCommand(CustomKitAddon plugin) {
        this.plugin = plugin;

        // Register as listener to intercept commands
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

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
                    page = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                    page = 1;
                }
            }

            // Open custom items GUI directly
            plugin.getGuiManager().openCustomItemsGUI(player, page, -1);
        }
    }
}