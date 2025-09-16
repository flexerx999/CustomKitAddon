package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {

    private final CustomKitAddon plugin;
    private final Map<UUID, CustomItemsGUI> openGuis = new HashMap<>();
    private final Map<UUID, Integer> selectedSlots = new HashMap<>();

    public GUIManager(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    public void openCustomItemsGUI(Player player, int page, int selectedSlot) {
        // Store the selected slot for later use
        selectedSlots.put(player.getUniqueId(), selectedSlot);

        CustomItemsGUI gui = new CustomItemsGUI(plugin, player, page);
        openGuis.put(player.getUniqueId(), gui);
        gui.open();
    }

    public void openEnchantmentGUI(Player player, ItemStack armorPiece) {
        EnchantmentGUI gui = new EnchantmentGUI(plugin, player, armorPiece);
        gui.open();
    }

    public void closeGUI(Player player) {
        openGuis.remove(player.getUniqueId());
        selectedSlots.remove(player.getUniqueId());
    }

    public boolean hasOpenGUI(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    public CustomItemsGUI getOpenGUI(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    public int getSelectedSlot(Player player) {
        return selectedSlots.getOrDefault(player.getUniqueId(), -1);
    }

    public void clearCache() {
        openGuis.clear();
        selectedSlots.clear();
    }
}