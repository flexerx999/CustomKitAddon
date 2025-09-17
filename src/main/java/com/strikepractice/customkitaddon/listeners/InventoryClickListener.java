package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.gui.CustomItemsGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    private final CustomKitAddon plugin;

    public InventoryClickListener(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Get title - handle both old and new API
        String title;
        String componentTitle = "";
        try {
            // Try new API first (1.20.5+) - get the full component
            componentTitle = event.getView().title().toString();
            // Extract readable text from component
            if (componentTitle.contains("CUSTOM KIT") || componentTitle.contains("Custom Kit")) {
                title = "Custom Kit";
            } else {
                title = event.getView().getTitle();
            }
        } catch (NoSuchMethodError e) {
            // Fallback to legacy method
            title = event.getView().getTitle();
        }

        // Debug to find the actual title
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Inventory clicked with title: " + componentTitle);
            plugin.getLogger().info("Clicked slot: " + event.getSlot());
        }

        // Handle custom items GUI clicks
        if (title.contains("Custom Kit Items - Page") ||
                componentTitle.contains("Custom Kit Items - Page")) {
            event.setCancelled(true);

            CustomItemsGUI gui = plugin.getGuiManager().getOpenGUI(player);
            if (gui != null) {
                gui.handleClick(event.getSlot());
            }
            return;
        }

        // Handle enchantment GUI clicks
        if (title.equals("§8Armor Enchantments")) {
            event.setCancelled(true);
            // Handle enchantment GUI clicks here
            return;
        }

        // INTERCEPT STRIKEPRACTICE'S ITEM SELECTION GUIs
        if (title.equals("Custom Kit Helmets") ||
                title.equals("Custom Kit Chestplates") ||
                title.equals("Custom Kit Leggings") ||
                title.equals("Custom Kit Boots") ||
                title.equals("Custom Kit Swords") ||
                title.equals("Custom Kit Items") ||
                title.contains("Custom Kit ")) {

            // Cancel StrikePractice's GUI and open ours instead
            event.setCancelled(true);
            player.closeInventory();

            // Get the original slot they clicked in the main GUI
            int originalSlot = plugin.getGuiManager().getSelectedSlot(player);
            if (originalSlot == -1) {
                // Try to determine slot from the category
                originalSlot = event.getSlot(); // Use current slot as fallback
            }

            // Make it final for lambda
            final int finalSlot = originalSlot;

            // Open our custom GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getGuiManager().openCustomItemsGUI(player, 1, finalSlot);
            }, 1L);

            return;
        }

        // Detect main StrikePractice customkit GUI
        if (componentTitle.contains("CUSTOM KIT") || componentTitle.contains("á´„á´œêœ±á´›á´á´ á´‹Éªá´›")) {
            // Store the slot when clicking in main customkit GUI
            plugin.getGuiManager().setSelectedSlot(player, event.getSlot());

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Main customkit GUI clicked, slot: " + event.getSlot());
            }
        }
    }

    private boolean isStrikePracticeCustomKitGUI(String title) {
        // Check for StrikePractice custom kit GUI title
        // Common titles in StrikePractice for custom kit editing
        return title.toLowerCase().contains("custom") &&
                (title.toLowerCase().contains("kit") ||
                        title.toLowerCase().contains("loadout") ||
                        title.toLowerCase().contains("edit"));
    }

    private void handleStrikePracticeClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();

        // Check if clicking an empty slot or air block
        if (clicked == null || clicked.getType() == Material.AIR) {
            int slot = event.getSlot();

            // Check if this is a valid equipment slot (0-35 typically for inventory)
            if (slot >= 0 && slot < 36) {
                event.setCancelled(true);

                // Open our custom items GUI
                plugin.getGuiManager().openCustomItemsGUI(player, 1, slot);
            }
        }

        // Check if clicking on armor piece for enchantments
        else if (isArmorPiece(clicked)) {
            // Check if right-click or shift-click (customize enchantments)
            if (event.isRightClick() || event.isShiftClick()) {
                event.setCancelled(true);
                plugin.getGuiManager().openEnchantmentGUI(player, clicked);
            }
        }

        // Check if clicking the rename button
        else if (isRenameButton(clicked)) {
            event.setCancelled(true);
            startRenameProcess(player);
        }
    }

    private boolean isArmorPiece(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private boolean isRenameButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        // Check if it's likely the rename button
        // You may need to adjust based on actual StrikePractice implementation
        String displayName = item.getItemMeta().getDisplayName();
        return displayName != null &&
                (displayName.toLowerCase().contains("rename") ||
                        displayName.toLowerCase().contains("name"));
    }

    private void startRenameProcess(Player player) {
        player.closeInventory();

        // Start chat-based rename
        plugin.getChatListener().startRename(player);

        player.sendMessage(plugin.getConfigManager().getMessage("rename-prompt"));
        player.sendMessage(plugin.getConfigManager().getMessage("rename-cancel-info"));
    }
}