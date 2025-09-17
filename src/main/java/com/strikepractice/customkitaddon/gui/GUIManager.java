package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.entity.Player;

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
        // Close any existing GUI first
        if (openGuis.containsKey(player.getUniqueId())) {
            openGuis.remove(player.getUniqueId());
        }

        // Store the selected slot for later use
        selectedSlots.put(player.getUniqueId(), selectedSlot);

        // Create and open the GUI
        CustomItemsGUI gui = new CustomItemsGUI(plugin, player, page, selectedSlot);
        openGuis.put(player.getUniqueId(), gui);
        gui.open();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Opened CustomItemsGUI for " + player.getName() +
                    " - Page: " + page + ", Slot: " + selectedSlot);
        }
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

    public void setSelectedSlot(Player player, int slot) {
        selectedSlots.put(player.getUniqueId(), slot);
    }

    public void clearCache() {
        openGuis.clear();
        selectedSlots.clear();
    }
}