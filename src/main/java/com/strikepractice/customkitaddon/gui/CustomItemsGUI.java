package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.models.CustomItem;
import com.strikepractice.customkitaddon.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItemsGUI {

    private final CustomKitAddon plugin;
    private final Player player;
    private final int page;
    private Inventory inventory;

    public CustomItemsGUI(CustomKitAddon plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(1, Math.min(page, 3)); // Ensure page is between 1-3
    }

    public void open() {
        String title = plugin.getConfigManager().getGuiTitle(page);
        inventory = Bukkit.createInventory(null, 54, title);

        // Fill with items
        fillItems();

        // Add navigation and decoration
        addBottomBar();

        player.openInventory(inventory);
    }

    private void fillItems() {
        List<CustomItem> items = plugin.getItemsConfig().getPageItems(page);

        for (CustomItem customItem : items) {
            if (customItem.hasSpecificSlot() && customItem.getSlot() < 45) {
                inventory.setItem(customItem.getSlot(), customItem.getItemStack());
            } else {
                // Find first empty slot in main area (0-44)
                for (int i = 0; i < 45; i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, customItem.getItemStack());
                        break;
                    }
                }
            }
        }
    }

    private void addBottomBar() {
        // Fill bottom row with gray stained glass
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("§7")
                .build();

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, grayGlass);
        }

        // Previous page button (slot 48 - 4th slot of bottom row)
        if (page > 1) {
            ItemStack prevButton = new ItemBuilder(Material.ARROW)
                    .setName("§a« Previous Page")
                    .setLore("§7Click to go to page " + (page - 1))
                    .build();
            inventory.setItem(48, prevButton);
        }

        // Page indicator (slot 49 - middle)
        ItemStack pageIndicator = new ItemBuilder(Material.BOOK)
                .setName("§ePage " + page + " of 3")
                .setLore("§7Browse custom kit items")
                .build();
        inventory.setItem(49, pageIndicator);

        // Next page button (slot 50 - 6th slot of bottom row)
        if (page < 3) {
            ItemStack nextButton = new ItemBuilder(Material.ARROW)
                    .setName("§aNext Page »")
                    .setLore("§7Click to go to page " + (page + 1))
                    .build();
            inventory.setItem(50, nextButton);
        }
    }

    public void handleClick(int slot) {
        // Prevent item grabbing
        if (slot < 45) {
            // Clicked an item slot
            ItemStack clickedItem = inventory.getItem(slot);
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                selectItem(clickedItem);
            }
            return;
        }

        // Check if clicking navigation buttons
        if (slot == 48 && page > 1) {
            // Previous page
            plugin.getGuiManager().openCustomItemsGUI(player, page - 1,
                    plugin.getGuiManager().getSelectedSlot(player));
            playSound();
        } else if (slot == 50 && page < 3) {
            // Next page
            plugin.getGuiManager().openCustomItemsGUI(player, page + 1,
                    plugin.getGuiManager().getSelectedSlot(player));
            playSound();
        } else if (slot < 45) {
            // Clicked an item slot
            ItemStack clickedItem = inventory.getItem(slot);
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                selectItem(clickedItem);
            }
        }
    }

    private void selectItem(ItemStack item) {
        int targetSlot = plugin.getGuiManager().getSelectedSlot(player);

        if (targetSlot >= 0) {
            // Apply item to the selected slot in StrikePractice custom kit
            applyItemToKit(item, targetSlot);

            // Play sound and close
            playSound();
            player.closeInventory();

            // Reopen StrikePractice customkit GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.performCommand("customkit");
            }, 1L);
        }
    }

    private void applyItemToKit(ItemStack item, int slot) {
        try {
            // Get the player's custom kit from StrikePractice API
            // This is a simplified version - you may need to adjust based on actual API
            ga.strikepractice.api.StrikePracticeAPI api = plugin.getStrikePracticeAPI();

            // Update the kit slot
            // Note: This assumes there's a method to update custom kits
            // You may need to adjust based on the actual StrikePractice API

            player.sendMessage(plugin.getConfigManager().getMessage("item-selected")
                    .replace("%item%", item.getType().name()));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply item to kit: " + e.getMessage());
            player.sendMessage(plugin.getConfigManager().getMessage("error-applying-item"));
        }
    }

    private void playSound() {
        if (plugin.getConfigManager().isSoundEnabled()) {
            try {
                Sound sound = Sound.valueOf(plugin.getConfigManager().getSelectSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {
                // Invalid sound, ignore
            }
        }
    }

    public int getPage() {
        return page;
    }
}