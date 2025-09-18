package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.events.KitSelectEvent;
import ga.strikepractice.events.KitDeselectEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StrikePracticeListener implements Listener {

    private final CustomKitAddon plugin;
    private final Set<UUID> bypassPlayers = new HashSet<>();

    public StrikePracticeListener(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // Skip if we're opening our own GUI
        if (bypassPlayers.contains(player.getUniqueId())) {
            bypassPlayers.remove(player.getUniqueId());
            return;
        }

        String title = event.getView().getTitle();

        // Intercept StrikePractice's item selection GUIs
        if (title.contains("Custom Kit") &&
                (title.contains("Helmets") || title.contains("Chestplates") ||
                        title.contains("Leggings") || title.contains("Boots") ||
                        title.contains("Swords") || title.contains("Items"))) {

            event.setCancelled(true);

            // Get the slot that was clicked in the main GUI
            int slot = plugin.getGuiManager().getSelectedSlot(player);
            if (slot == -1) slot = 0;

            // Mark player to bypass next check
            bypassPlayers.add(player.getUniqueId());

            // Open our GUI instead
            plugin.getGuiManager().openCustomItemsGUI(player, 1, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // Clean up any open GUIs
        if (plugin.getGuiManager().hasOpenGUI(player)) {
            plugin.getGuiManager().closeGUI(player);
        }

        plugin.getChatListener().cancelRename(player);
    }

    @EventHandler
    public void onKitSelect(KitSelectEvent event) {
        Player player = event.getPlayer();
        String kitName = event.getKit().getName();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(player.getName() + " selected kit: " + kitName);
        }
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