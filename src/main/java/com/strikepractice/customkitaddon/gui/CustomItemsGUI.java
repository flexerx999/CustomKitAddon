package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.models.CustomItem;
import com.strikepractice.customkitaddon.utils.ItemBuilder;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.playerkits.PlayerKits;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItemsGUI {

    private final CustomKitAddon plugin;
    private final Player player;
    private final int page;
    private final int targetSlot;
    private Inventory inventory;

    public CustomItemsGUI(CustomKitAddon plugin, Player player, int page, int targetSlot) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(1, Math.min(page, 3));
        this.targetSlot = targetSlot;
    }

    public void open() {
        String title = plugin.getConfigManager().getGuiTitle(page);
        inventory = Bukkit.createInventory(null, 54, title);

        // Fill with items
        fillItems();

        // Add navigation buttons at bottom
        addBottomBar();

        player.openInventory(inventory);
    }

    private void fillItems() {
        List<CustomItem> items = plugin.getItemsConfig().getPageItems(page);

        for (CustomItem customItem : items) {
            if (customItem.hasSpecificSlot() && customItem.getSlot() < 45) {
                inventory.setItem(customItem.getSlot(), customItem.getItemStack());
            } else {
                // Find first empty slot
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
        // Fill bottom row with gray glass
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("§7")
                .build();

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, grayGlass);
        }

        // Previous page button (slot 48)
        if (page > 1) {
            ItemStack prevButton = new ItemBuilder(Material.ARROW)
                    .setName("§a« Previous Page")
                    .setLore("§7Click to go to page " + (page - 1))
                    .build();
            inventory.setItem(48, prevButton);
        }

        // Page indicator (slot 49)
        ItemStack pageIndicator = new ItemBuilder(Material.BOOK)
                .setName("§ePage " + page + " of 3")
                .setLore("§7Browse custom kit items")
                .build();
        inventory.setItem(49, pageIndicator);

        // Next page button (slot 50)
        if (page < plugin.getItemsConfig().getTotalPages()) {
            ItemStack nextButton = new ItemBuilder(Material.ARROW)
                    .setName("§aNext Page »")
                    .setLore("§7Click to go to page " + (page + 1))
                    .build();
            inventory.setItem(50, nextButton);
        }
    }

    public void handleClick(int slot) {
        if (slot < 0 || slot >= 54) return;

        // Navigation buttons
        if (slot == 48 && page > 1) {
            // Previous page
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Navigating to previous page: " + (page - 1));
            }

            // Open new page with a delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openCustomItemsGUI(player, page - 1, targetSlot);
                }, 1L);
            }, 1L);

            playSound();
            return;
        }

        if (slot == 50 && page < 3) {
            // Next page
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Navigating to next page: " + (page + 1));
            }

            // Open new page with a delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openCustomItemsGUI(player, page + 1, targetSlot);
                }, 1L);
            }, 1L);

            playSound();
            return;
        }

        // Item selection (slots 0-44)
        if (slot < 45) {
            ItemStack clickedItem = inventory.getItem(slot);
            if (clickedItem != null && clickedItem.getType() != Material.AIR &&
                    clickedItem.getType() != Material.GRAY_STAINED_GLASS_PANE) {

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Selected item: " + clickedItem.getType() +
                            " for slot " + targetSlot);
                }

                selectItem(clickedItem);
            }
        }
    }

    private void selectItem(ItemStack item) {
        // targetSlot is already the INVENTORY slot (0-35) thanks to our mapping
        if (targetSlot >= 0 && targetSlot <= 35) {
            // Apply item to the kit at the mapped inventory slot
            boolean success = applyItemToKit(item, targetSlot);

            if (success) {
                // Play sound and close
                playSound();
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                // Send success message
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().toString();

                String message = plugin.getConfigManager().getMessage("item-selected")
                        .replace("%item%", itemName);
                player.sendMessage(message);

                // Reopen StrikePractice customkit GUI
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().closeGUI(player);
                    player.performCommand("customkit");
                }, 1L);
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("error-applying-item"));
            }
        }
    }

    private boolean applyItemToKit(ItemStack item, int slot) {
        try {
            StrikePracticeAPI api = plugin.getStrikePracticeAPI();
            PlayerKits playerKits = api.getPlayerKits(player);

            // Get the current custom kit
            BattleKit customKit = playerKits.getCustomKit();

            if (customKit == null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("No custom kit found for player");
                }
                return false;
            }

            // Get the kit inventory
            List<ItemStack> kitItems = customKit.getInventory();

            // Ensure list is large enough
            while (kitItems.size() <= slot) {
                kitItems.add(new ItemStack(Material.AIR));
            }

            // Set the item at the specified INVENTORY slot (0-35)
            kitItems.set(slot, item.clone());

            // The changes should be automatically saved since we're modifying the list directly

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Successfully applied " + item.getType() + " to inventory slot " + slot);
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply item to kit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void playSound() {
        if (plugin.getConfigManager().isSoundEnabled()) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }
    }

    public Player getPlayer() {
        return player;
    }

    public int getPage() {
        return page;
    }
}