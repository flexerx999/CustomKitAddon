package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.events.KitSelectEvent;
import ga.strikepractice.events.KitDeselectEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;

public class StrikePracticeListener implements Listener {

    private final CustomKitAddon plugin;

    public StrikePracticeListener(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKitSelect(KitSelectEvent event) {
        Player player = event.getPlayer();
        String kitName = event.getKit().getName();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(player.getName() + " selected kit: " + kitName);
        }

        // You can add custom logic here when a kit is selected
    }

    @EventHandler
    public void onKitDeselect(KitDeselectEvent event) {
        Player player = event.getPlayer();

        // Clean up any open GUIs
        if (plugin.getGuiManager().hasOpenGUI(player)) {
            plugin.getGuiManager().closeGUI(player);
        }

        // Cancel any rename sessions
        plugin.getChatListener().cancelRename(player);
    }
}