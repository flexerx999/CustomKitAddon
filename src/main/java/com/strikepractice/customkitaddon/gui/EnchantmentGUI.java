package com.strikepractice.customkitaddon.gui;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.utils.ItemBuilder;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.playerkits.PlayerKits;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EnchantmentGUI {

    private final CustomKitAddon plugin;
    private final Player player;
    private final ItemStack baseItem;
    private final int targetSlot;
    private Inventory inventory;

    // Track selected enchantments and their levels
    private final Map<Enchantment, Integer> selectedEnchants = new HashMap<>();
    private final Map<Enchantment, Integer> maxLevels = new HashMap<>();

    // Enchantment display slots
    private final Map<Integer, Enchantment> slotToEnchant = new HashMap<>();

    public EnchantmentGUI(CustomKitAddon plugin, Player player, ItemStack baseItem, int targetSlot) {
        this.plugin = plugin;
        this.player = player;
        this.baseItem = baseItem.clone();
        this.targetSlot = targetSlot;

        // Remove existing enchantments from base item
        for (Enchantment ench : this.baseItem.getEnchantments().keySet()) {
            this.baseItem.removeEnchantment(ench);
        }

        loadValidEnchantments();
    }

    private void loadValidEnchantments() {
        // Get all enchantments valid for this item
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.canEnchantItem(baseItem)) {
                maxLevels.put(enchantment, enchantment.getMaxLevel());
                selectedEnchants.put(enchantment, 0); // Start at level 0 (OFF)
            }
        }
    }

    public void open() {
        int size = 54; // 6 rows
        String title = "§8Enchantment Selection";
        inventory = Bukkit.createInventory(null, size, title);

        setupEnchantmentItems();
        updatePreviewItem();

        player.openInventory(inventory);
    }

    private void setupEnchantmentItems() {
        // Fill background with gray glass
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("§7")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, grayGlass);
        }

        // Place enchantments starting from slot 10
        int slot = 10;
        int row = 1;
        int col = 1;

        for (Enchantment enchantment : selectedEnchants.keySet()) {
            if (slot >= 44) break; // Don't go past slot 44

            // Skip to next row if needed (7 items per row, leaving borders)
            if (col > 7) {
                col = 1;
                row++;
                slot = row * 9 + 1;
            }

            // Skip if we're in the last row
            if (row >= 5) break;

            slotToEnchant.put(slot, enchantment);
            updateEnchantmentItem(slot, enchantment);

            col++;
            slot++;
        }

        // Add control buttons at bottom
        setupBottomBar();
    }

    private void updateEnchantmentItem(int slot, Enchantment enchantment) {
        int currentLevel = selectedEnchants.get(enchantment);
        int maxLevel = maxLevels.get(enchantment);

        Material displayMaterial = getEnchantmentDisplayMaterial(enchantment);
        String enchantName = getEnchantmentDisplayName(enchantment);

        ItemBuilder builder = new ItemBuilder(displayMaterial);

        if (currentLevel == 0) {
            // OFF state
            builder.setName("§7" + enchantName + ": §cOFF");
            builder.setLore(
                    "§7Current: §cDisabled",
                    "§7Max Level: §e" + toRoman(maxLevel),
                    "",
                    "§eClick to enable!"
            );
        } else {
            // ON state with level
            builder.setName("§b" + enchantName + ": §a" + toRoman(currentLevel));
            builder.setLore(
                    "§7Current Level: §a" + toRoman(currentLevel),
                    "§7Max Level: §e" + toRoman(maxLevel),
                    "",
                    "§eClick to increase level!",
                    "§cShift-Click to disable!"
            );

            // Add enchantment glow effect
            builder.addEnchantment(Enchantment.UNBREAKING, 1);
            builder.addItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        inventory.setItem(slot, builder.build());
    }

    private void setupBottomBar() {
        // Clear button (slot 48)
        ItemStack clearButton = new ItemBuilder(Material.BARRIER)
                .setName("§c§lClear All")
                .setLore("§7Remove all enchantments")
                .build();
        inventory.setItem(48, clearButton);

        // Preview/Confirm item (slot 49)
        updatePreviewItem();

        // Cancel button (slot 50)
        ItemStack cancelButton = new ItemBuilder(Material.RED_WOOL)
                .setName("§c§lCancel")
                .setLore("§7Return without applying")
                .build();
        inventory.setItem(50, cancelButton);
    }

    private void updatePreviewItem() {
        ItemStack preview = baseItem.clone();

        // Apply selected enchantments
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchants.entrySet()) {
            if (entry.getValue() > 0) {
                preview.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        // Update lore to show it's the confirm button
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§a§l✓ CLICK TO CONFIRM");
            lore.add("§7Adds this item to your kit");
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }

        inventory.setItem(49, preview);
    }

    public void handleClick(int slot) {
        // Check if it's an enchantment slot
        if (slotToEnchant.containsKey(slot)) {
            Enchantment enchantment = slotToEnchant.get(slot);
            cycleEnchantmentLevel(enchantment);
            updateEnchantmentItem(slot, enchantment);
            updatePreviewItem();
            playSound();
            return;
        }

        // Handle bottom bar buttons
        switch (slot) {
            case 48: // Clear all
                clearAllEnchantments();
                playSound();
                break;

            case 49: // Confirm/Apply
                confirmSelection();
                break;

            case 50: // Cancel
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                playSound();
                break;
        }
    }

    private void cycleEnchantmentLevel(Enchantment enchantment) {
        int currentLevel = selectedEnchants.get(enchantment);
        int maxLevel = maxLevels.get(enchantment);

        // Cycle: OFF (0) → I (1) → II (2) → ... → Max → OFF (0)
        currentLevel++;
        if (currentLevel > maxLevel) {
            currentLevel = 0;
        }

        selectedEnchants.put(enchantment, currentLevel);
    }

    private void clearAllEnchantments() {
        for (Enchantment enchantment : selectedEnchants.keySet()) {
            selectedEnchants.put(enchantment, 0);
        }

        // Refresh all enchantment items
        for (Map.Entry<Integer, Enchantment> entry : slotToEnchant.entrySet()) {
            updateEnchantmentItem(entry.getKey(), entry.getValue());
        }

        updatePreviewItem();
    }

    private void confirmSelection() {
        // Create final item with selected enchantments
        ItemStack finalItem = baseItem.clone();
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchants.entrySet()) {
            if (entry.getValue() > 0) {
                finalItem.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        // Apply to kit using the same logic as CustomItemsGUI
        applyItemToKit(finalItem);

        // Close and return to main customkit GUI
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

        // Play success sound
        playSound();

        // Send success message
        String itemName = finalItem.hasItemMeta() && finalItem.getItemMeta().hasDisplayName()
                ? finalItem.getItemMeta().getDisplayName()
                : finalItem.getType().toString();

        String message = plugin.getConfigManager().getMessage("item-selected")
                .replace("%item%", itemName + " §7(Enchanted)");
        player.sendMessage(message);

        // Reopen StrikePractice customkit GUI
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getGuiManager().closeGUI(player);
            player.performCommand("customkit");
        }, 1L);
    }

    private boolean applyItemToKit(ItemStack item) {
        try {
            StrikePracticeAPI api = plugin.getStrikePracticeAPI();
            PlayerKits playerKits = api.getPlayerKits(player);
            BattleKit customKit = playerKits.getCustomKit();

            if (customKit == null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("No custom kit found for player");
                }
                return false;
            }

            List<ItemStack> kitItems = customKit.getInventory();
            while (kitItems.size() <= targetSlot) {
                kitItems.add(new ItemStack(Material.AIR));
            }

            kitItems.set(targetSlot, item.clone());

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Successfully applied enchanted " + item.getType() + " to slot " + targetSlot);
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply item to kit: " + e.getMessage());
            return false;
        }
    }

    private void playSound() {
        if (plugin.getConfigManager().isSoundEnabled()) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } catch (Exception ignored) {}
        }
    }

    private Material getEnchantmentDisplayMaterial(Enchantment enchantment) {
        // Return appropriate display materials for each enchantment
        String key = enchantment.getKey().getKey().toUpperCase();

        // Damage enchantments
        if (key.contains("SHARPNESS") || key.contains("DAMAGE")) return Material.IRON_SWORD;
        if (key.contains("SMITE")) return Material.GOLDEN_SWORD;
        if (key.contains("BANE")) return Material.WOODEN_SWORD;
        if (key.contains("POWER")) return Material.BOW;
        if (key.contains("IMPALING")) return Material.TRIDENT;

        // Protection enchantments
        if (key.contains("PROTECTION")) return Material.IRON_CHESTPLATE;
        if (key.contains("FIRE_PROTECTION")) return Material.GOLDEN_CHESTPLATE;
        if (key.contains("BLAST")) return Material.TNT;
        if (key.contains("PROJECTILE")) return Material.ARROW;
        if (key.contains("FEATHER")) return Material.FEATHER;
        if (key.contains("RESPIRATION") || key.contains("AQUA")) return Material.WATER_BUCKET;

        // Tool enchantments
        if (key.contains("EFFICIENCY") || key.contains("DIG")) return Material.GOLDEN_PICKAXE;
        if (key.contains("SILK")) return Material.STRING;
        if (key.contains("FORTUNE")) return Material.EMERALD;
        if (key.contains("LOOT")) return Material.GOLD_INGOT;

        // Durability
        if (key.contains("UNBREAKING") || key.contains("DURABILITY")) return Material.ANVIL;
        if (key.contains("MENDING")) return Material.EXPERIENCE_BOTTLE;

        // Special enchantments
        if (key.contains("THORNS")) return Material.CACTUS;
        if (key.contains("FIRE_ASPECT")) return Material.BLAZE_POWDER;
        if (key.contains("KNOCKBACK")) return Material.PISTON;
        if (key.contains("SWEEPING")) return Material.IRON_BARS;
        if (key.contains("LOYALTY")) return Material.LEAD;
        if (key.contains("RIPTIDE")) return Material.WATER_BUCKET;
        if (key.contains("CHANNELING")) return Material.LIGHTNING_ROD;
        if (key.contains("MULTISHOT")) return Material.ARROW;
        if (key.contains("PIERCING")) return Material.SPECTRAL_ARROW;
        if (key.contains("QUICK_CHARGE")) return Material.SUGAR;
        if (key.contains("SOUL_SPEED")) return Material.SOUL_SAND;
        if (key.contains("SWIFT_SNEAK")) return Material.LEATHER_BOOTS;
        if (key.contains("FROST_WALKER")) return Material.ICE;
        if (key.contains("DEPTH_STRIDER")) return Material.PRISMARINE_SHARD;
        if (key.contains("BINDING_CURSE")) return Material.CHAIN;
        if (key.contains("VANISHING_CURSE")) return Material.ENDER_PEARL;
        if (key.contains("INFINITY")) return Material.SPECTRAL_ARROW;
        if (key.contains("FLAME")) return Material.BLAZE_POWDER;
        if (key.contains("PUNCH")) return Material.SLIME_BALL;

        // Default
        return Material.ENCHANTED_BOOK;
    }

    private String getEnchantmentDisplayName(Enchantment enchantment) {
        // Convert enchantment key to readable name
        String key = enchantment.getKey().getKey();

        // Special cases for better names
        if (key.equals("sharpness")) return "Sharpness";
        if (key.equals("smite")) return "Smite";
        if (key.equals("bane_of_arthropods")) return "Bane of Arthropods";
        if (key.equals("knockback")) return "Knockback";
        if (key.equals("fire_aspect")) return "Fire Aspect";
        if (key.equals("looting")) return "Looting";
        if (key.equals("sweeping")) return "Sweeping Edge";
        if (key.equals("efficiency")) return "Efficiency";
        if (key.equals("silk_touch")) return "Silk Touch";
        if (key.equals("unbreaking")) return "Unbreaking";
        if (key.equals("fortune")) return "Fortune";
        if (key.equals("power")) return "Power";
        if (key.equals("punch")) return "Punch";
        if (key.equals("flame")) return "Flame";
        if (key.equals("infinity")) return "Infinity";
        if (key.equals("protection")) return "Protection";
        if (key.equals("fire_protection")) return "Fire Protection";
        if (key.equals("feather_falling")) return "Feather Falling";
        if (key.equals("blast_protection")) return "Blast Protection";
        if (key.equals("projectile_protection")) return "Projectile Protection";
        if (key.equals("respiration")) return "Respiration";
        if (key.equals("aqua_affinity")) return "Aqua Affinity";
        if (key.equals("thorns")) return "Thorns";
        if (key.equals("depth_strider")) return "Depth Strider";
        if (key.equals("frost_walker")) return "Frost Walker";
        if (key.equals("soul_speed")) return "Soul Speed";
        if (key.equals("swift_sneak")) return "Swift Sneak";
        if (key.equals("binding_curse")) return "Curse of Binding";
        if (key.equals("vanishing_curse")) return "Curse of Vanishing";
        if (key.equals("mending")) return "Mending";
        if (key.equals("loyalty")) return "Loyalty";
        if (key.equals("impaling")) return "Impaling";
        if (key.equals("riptide")) return "Riptide";
        if (key.equals("channeling")) return "Channeling";
        if (key.equals("multishot")) return "Multishot";
        if (key.equals("quick_charge")) return "Quick Charge";
        if (key.equals("piercing")) return "Piercing";
        if (key.equals("luck_of_the_sea")) return "Luck of the Sea";
        if (key.equals("lure")) return "Lure";

        // Fallback: capitalize and replace underscores
        return key.substring(0, 1).toUpperCase() +
                key.substring(1).replace("_", " ");
    }

    private String toRoman(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(number);
        }
    }

    public Player getPlayer() {
        return player;
    }
}