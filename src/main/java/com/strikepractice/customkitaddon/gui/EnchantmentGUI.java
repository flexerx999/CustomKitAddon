package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EnchantmentGUI {

    private final CustomKitAddon plugin;
    private final Player player;
    private final ItemStack armorPiece;
    private Inventory inventory;

    public EnchantmentGUI(CustomKitAddon plugin, Player player, ItemStack armorPiece) {
        this.plugin = plugin;
        this.player = player;
        this.armorPiece = armorPiece.clone();
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§8Armor Enchantments");

        // Add existing enchantment options
        addProtectionOptions();
        addUnbreakingOptions();

        // Add new Mending option
        if (plugin.getConfigManager().isMendingEnabled()) {
            addMendingOption();
        }

        // Add the armor piece in center
        inventory.setItem(13, armorPiece);

        // Add apply button
        ItemStack applyButton = new ItemBuilder(Material.EMERALD_BLOCK)
                .setName("§a§lApply Enchantments")
                .setLore("§7Click to apply selected enchantments")
                .build();
        inventory.setItem(22, applyButton);

        // Fill empty slots with glass
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName("§7")
                .build();
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }

        player.openInventory(inventory);
    }

    private void addProtectionOptions() {
        // Protection levels
        for (int level = 1; level <= 4; level++) {
            ItemStack protItem = new ItemBuilder(Material.IRON_CHESTPLATE)
                    .setName("§bProtection " + level)
                    .setLore("§7Click to add Protection " + level)
                    .addEnchantment(Enchantment.PROTECTION, level)
                    .build();
            inventory.setItem(level - 1, protItem);
        }
    }

    private void addUnbreakingOptions() {
        // Unbreaking levels
        for (int level = 1; level <= 3; level++) {
            ItemStack unbItem = new ItemBuilder(Material.ANVIL)
                    .setName("§6Unbreaking " + level)
                    .setLore("§7Click to add Unbreaking " + level)
                    .build();
            inventory.setItem(9 + level - 1, unbItem);
        }
    }

    private void addMendingOption() {
        int slot = plugin.getConfigManager().getMendingSlot();

        ItemStack mendingItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName("§d§lMending")
                .setLore("§7Click to add Mending enchantment",
                        "§7Repairs armor with experience")
                .addEnchantment(Enchantment.MENDING, 1)
                .build();

        inventory.setItem(slot, mendingItem);
    }

    public void handleClick(int slot) {
        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Handle Protection clicks
        if (slot >= 0 && slot <= 3) {
            int level = slot + 1;
            toggleEnchantment(Enchantment.PROTECTION, level);
            playSound();
        }

        // Handle Unbreaking clicks
        else if (slot >= 9 && slot <= 11) {
            int level = slot - 8;
            toggleEnchantment(Enchantment.UNBREAKING, level);
            playSound();
        }

        // Handle Mending click
        else if (slot == plugin.getConfigManager().getMendingSlot()) {
            toggleEnchantment(Enchantment.MENDING, 1);
            playSound();
        }

        // Handle Apply button
        else if (slot == 22) {
            applyEnchantments();
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("enchantments-applied"));
            playSound();
        }

        // Update display
        if (slot != 22) {
            inventory.setItem(13, armorPiece);
        }
    }

    private void toggleEnchantment(Enchantment enchantment, int level) {
        if (armorPiece.containsEnchantment(enchantment)) {
            armorPiece.removeEnchantment(enchantment);
        } else {
            // Remove other levels of the same enchantment first
            armorPiece.removeEnchantment(enchantment);
            armorPiece.addUnsafeEnchantment(enchantment, level);
        }
    }

    private void applyEnchantments() {
        // Apply the enchanted armor piece to the player's custom kit
        try {
            ga.strikepractice.api.StrikePracticeAPI api = plugin.getStrikePracticeAPI();

            // Get the player's kit (using available method)
            ga.strikepractice.battlekit.BattleKit customKit = api.getKit(player);

            if (customKit != null) {
                // Get the kit items (returns List<ItemStack>)
                java.util.List<org.bukkit.inventory.ItemStack> kitItems = customKit.getInventory();

                // Find the appropriate armor slot
                int targetSlot = -1;
                String type = armorPiece.getType().name();

                // Armor slots in inventory: 36=boots, 37=leggings, 38=chestplate, 39=helmet
                if (type.endsWith("_HELMET")) targetSlot = 39;
                else if (type.endsWith("_CHESTPLATE")) targetSlot = 38;
                else if (type.endsWith("_LEGGINGS")) targetSlot = 37;
                else if (type.endsWith("_BOOTS")) targetSlot = 36;

                if (targetSlot != -1) {
                    // Ensure list is large enough
                    while (kitItems.size() <= targetSlot) {
                        kitItems.add(new ItemStack(Material.AIR));
                    }

                    // Set the enchanted armor piece
                    kitItems.set(targetSlot, armorPiece);

                    // The kit is automatically updated since we're modifying the list directly
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply enchantments: " + e.getMessage());
        }
    }

    private void playSound() {
        if (plugin.getConfigManager().isSoundEnabled()) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }
    }
}