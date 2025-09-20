package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {

    private final CustomKitAddon plugin;
    private final Map<UUID, CustomItemsGUI> openGuis = new HashMap<>();
    private final Map<UUID, EnchantmentGUI> enchantGuis = new HashMap<>();
    private final Map<UUID, Integer> selectedSlots = new HashMap<>();

    public GUIManager(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    public void openCustomItemsGUI(Player player, int page, int selectedSlot) {
        // Store the selected slot for later use
        selectedSlots.put(player.getUniqueId(), selectedSlot);

        // Create and open the GUI - ensure page is between 1 and max
        final int finalPage = Math.max(1, Math.min(page, plugin.getConfigManager().getTotalPages()));
        final int finalSelectedSlot = selectedSlot;

        // Clear any pending tasks for this player to prevent multiple GUIs opening
        if (player.hasMetadata("customkit_pending_task")) {
            int taskId = player.getMetadata("customkit_pending_task").get(0).asInt();
            Bukkit.getScheduler().cancelTask(taskId);
            player.removeMetadata("customkit_pending_task", plugin);
        }

        // Add a small delay before opening the new GUI to prevent overlap
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            CustomItemsGUI gui = new CustomItemsGUI(plugin, player, finalPage, finalSelectedSlot);
            openGuis.put(player.getUniqueId(), gui);
            gui.open();

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Opened CustomItemsGUI for " + player.getName() +
                        " - Page: " + finalPage + ", Slot: " + finalSelectedSlot);
            }

            // Remove the metadata after task completes
            player.removeMetadata("customkit_pending_task", plugin);
        }, 3L).getTaskId();

        // Store the task ID in metadata
        player.setMetadata("customkit_pending_task", new FixedMetadataValue(plugin, taskId));
    }

    public void openEnchantmentGUI(Player player, ItemStack baseItem, int targetSlot) {
        // Clear any pending tasks
        if (player.hasMetadata("customkit_pending_task")) {
            int taskId = player.getMetadata("customkit_pending_task").get(0).asInt();
            Bukkit.getScheduler().cancelTask(taskId);
            player.removeMetadata("customkit_pending_task", plugin);
        }

        // Create and open the enchantment GUI
        EnchantmentGUI gui = new EnchantmentGUI(plugin, player, baseItem, targetSlot);
        enchantGuis.put(player.getUniqueId(), gui);
        gui.open();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Opened EnchantmentGUI for " + player.getName() +
                    " - Item: " + baseItem.getType() + ", Target Slot: " + targetSlot);
        }
    }

    public void closeGUI(Player player) {
        openGuis.remove(player.getUniqueId());
        enchantGuis.remove(player.getUniqueId());
        selectedSlots.remove(player.getUniqueId());
    }

    public boolean hasOpenGUI(Player player) {
        return openGuis.containsKey(player.getUniqueId()) ||
                enchantGuis.containsKey(player.getUniqueId());
    }

    public CustomItemsGUI getOpenGUI(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    public EnchantmentGUI getEnchantmentGUI(Player player) {
        return enchantGuis.get(player.getUniqueId());
    }

    public int getSelectedSlot(Player player) {
        return selectedSlots.getOrDefault(player.getUniqueId(), -1);
    }

    public void setSelectedSlot(Player player, int slot) {
        selectedSlots.put(player.getUniqueId(), slot);
    }

    public void clearCache() {
        openGuis.clear();
        enchantGuis.clear();
        selectedSlots.clear();
    }
}