package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.gui.CustomItemsGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    private final CustomKitAddon plugin;

    public InventoryClickListener(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Get title - handle both old and new API
        String title;
        String componentTitle = "";
        try {
            // Try new API first (1.20.5+)
            componentTitle = event.getView().title().toString();
            title = event.getView().getTitle();
        } catch (NoSuchMethodError e) {
            // Fallback to legacy method
            title = event.getView().getTitle();
        }

        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Inventory clicked with title: " + componentTitle);
            plugin.getLogger().info("Clicked slot: " + slot);
        }

        // Handle our custom items GUI clicks FIRST (highest priority)
        CustomItemsGUI gui = plugin.getGuiManager().getOpenGUI(player);
        if (gui != null) {
            event.setCancelled(true);
            gui.handleClick(slot);
            return;
        }

        // Handle "Custom Kit Icon" title (might be rename GUI in some versions)
        if (componentTitle.contains("Custom Kit Icon")) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Detected Custom Kit Icon GUI - checking for rename");
            }

            // Force close inventory and start rename process
            event.setCancelled(true);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startRenameProcess(player);
            }, 2L);
            return;
        }

        // Detect main StrikePractice customkit GUI - be more inclusive with title detection
        // But exclude our own GUI
        if (!title.contains("Custom Kit Items - Page") &&
                !componentTitle.contains("Custom Kit Items - Page") &&
                (componentTitle.toUpperCase().contains("CUSTOM") ||
                        title.toUpperCase().contains("CUSTOM") ||
                        title.contains("ᴄᴜꜱᴛᴏᴍ") ||
                        componentTitle.contains("ᴄᴜꜱᴛᴏᴍ"))) {

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("StrikePractice GUI clicked at slot: " + slot);
            }

            // COMPLETELY IGNORE armor slots (0-3) - don't even process them
            if (slot >= 0 && slot <= 3) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Ignoring armor slot " + slot + " (handled by StrikePractice)");
                }
                // DO NOT cancel the event - let StrikePractice handle it
                return;
            }

            // Handle other slots (5-17) as before
            if (slot >= 5 && slot <= 17) {
                // Do nothing - let StrikePractice handle these slots
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Ignoring click on slot " + slot + " (handled by StrikePractice)");
                }
                return;
            }

            // Handle rename slot (4)
            if (slot == 4) {
                event.setCancelled(true);

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Rename slot clicked - force closing GUI");
                }

                // Force close inventory immediately
                player.closeInventory();

                // Start rename process after ensuring closure
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    startRenameProcess(player);
                }, 5L); // Increased delay to ensure GUI is fully closed
                return;
            }

            // ONLY handle item slots (18-53)
            if (slot >= 18 && slot <= 53) {
                event.setCancelled(true);

                // Map the GUI slot to inventory slot (18→0, 19→1, etc.)
                int inventorySlot = slot - 18;

                // Store the INVENTORY slot for later use (0-35)
                plugin.getGuiManager().setSelectedSlot(player, inventorySlot);

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Item slot clicked at GUI position " + slot +
                            " (maps to inventory slot " + inventorySlot + ")");
                }

                // Open items selection GUI with the mapped inventory slot
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openCustomItemsGUI(player, 1, inventorySlot);
                }, 1L);

            }
        }
    }

    private void startRenameProcess(Player player) {
        // Start chat-based rename
        plugin.getChatListener().startRename(player);

        player.sendMessage(plugin.getConfigManager().getMessage("rename-prompt"));
        player.sendMessage(plugin.getConfigManager().getMessage("rename-cancel-info"));
    }
}