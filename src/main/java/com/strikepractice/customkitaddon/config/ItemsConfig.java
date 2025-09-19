package com.strikepractice.customkitaddon.config;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.models.CustomItem;
import com.strikepractice.customkitaddon.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ItemsConfig {

    private final CustomKitAddon plugin;
    private FileConfiguration itemsConfig;
    private File itemsFile;
    private final Map<Integer, List<CustomItem>> pageItems = new HashMap<>();

    public ItemsConfig(CustomKitAddon plugin) {
        this.plugin = plugin;
        loadItems();
    }

    private void loadItems() {
        itemsFile = new File(plugin.getDataFolder(), "items.yml");

        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        loadItemsFromConfig();
    }

    private void loadItemsFromConfig() {
        pageItems.clear();

        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (int page = 1; page <= getTotalPages(); page++) {
            List<CustomItem> items = new ArrayList<>();
            ConfigurationSection pageSection = itemsSection.getConfigurationSection("page" + page);

            if (pageSection != null) {
                for (String key : pageSection.getKeys(false)) {
                    ConfigurationSection itemSection = pageSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        CustomItem customItem = loadCustomItem(itemSection);
                        if (customItem != null) {
                            items.add(customItem);
                        }
                    }
                }
            }

            pageItems.put(page, items);
        }
    }

    private CustomItem loadCustomItem(ConfigurationSection section) {
        try {
            String materialName = section.getString("material", "AIR");
            Material material = Material.valueOf(materialName.toUpperCase());

            if (material == Material.AIR) return null;

            ItemBuilder builder = new ItemBuilder(material);

            // Basic properties
            if (section.contains("amount")) {
                builder.setAmount(section.getInt("amount", 1));
            }

            if (section.contains("name")) {
                builder.setName(section.getString("name").replace("&", "§"));
            }

            if (section.contains("lore")) {
                List<String> lore = section.getStringList("lore");
                lore.replaceAll(s -> s.replace("&", "§"));
                builder.setLore(lore);
            }

            // Enchantments
            ConfigurationSection enchants = section.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String enchantName : enchants.getKeys(false)) {
                    try {
                        // Try to get by key first (new method)
                        Enchantment enchantment = Enchantment.getByKey(
                                org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase())
                        );

                        // Fallback to legacy name if needed
                        if (enchantment == null) {
                            enchantment = Enchantment.getByName(enchantName.toUpperCase());
                        }

                        if (enchantment != null) {
                            builder.addEnchantment(enchantment, enchants.getInt(enchantName));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid enchantment: " + enchantName);
                    }
                }
            }

            // Durability
            if (section.contains("durability")) {
                builder.setDurability((short) section.getInt("durability"));
            }

            // Custom model data (1.14+)
            if (section.contains("custom-model-data")) {
                builder.setCustomModelData(section.getInt("custom-model-data"));
            }

            // Attribute modifiers
            ConfigurationSection attributes = section.getConfigurationSection("attributes");
            if (attributes != null) {
                for (String attrName : attributes.getKeys(false)) {
                    ConfigurationSection attrSection = attributes.getConfigurationSection(attrName);
                    if (attrSection != null) {
                        builder.addAttributeModifier(
                                attrName,
                                attrSection.getDouble("amount", 0),
                                attrSection.getString("operation", "ADD_NUMBER"),
                                attrSection.getString("slot", "HAND")
                        );
                    }
                }
            }

            ItemStack item = builder.build();

            // Potion effects (for potions)
            if (material.name().contains("POTION")) {
                ConfigurationSection effects = section.getConfigurationSection("potion-effects");
                if (effects != null && item.getItemMeta() instanceof PotionMeta) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();

                    for (String effectName : effects.getKeys(false)) {
                        ConfigurationSection effectSection = effects.getConfigurationSection(effectName);
                        if (effectSection != null) {
                            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                            if (type != null) {
                                int duration = effectSection.getInt("duration", 600) * 20; // Convert to ticks
                                int amplifier = effectSection.getInt("amplifier", 0);
                                meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
                            }
                        }
                    }

                    item.setItemMeta(meta);
                }
            }

            int slot = section.getInt("slot", -1);
            return new CustomItem(item, slot);

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading item from config: " + e.getMessage());
            return null;
        }
    }

    public void addItem(int page, ItemStack item, int slot) {
        List<CustomItem> items = pageItems.computeIfAbsent(page, k -> new ArrayList<>());
        items.add(new CustomItem(item, slot));
        saveItem(page, items.size() - 1, item, slot);
    }

    private void saveItem(int page, int index, ItemStack item, int slot) {
        String path = "items.page" + page + ".item" + index;

        itemsConfig.set(path + ".material", item.getType().name());
        itemsConfig.set(path + ".amount", item.getAmount());
        itemsConfig.set(path + ".slot", slot);

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                itemsConfig.set(path + ".name", meta.getDisplayName().replace("§", "&"));
            }

            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>(meta.getLore());
                lore.replaceAll(s -> s.replace("§", "&"));
                itemsConfig.set(path + ".lore", lore);
            }
        }

        if (!item.getEnchantments().isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                // Use the key name for modern enchantment naming
                String enchantName = entry.getKey().getKey().getKey();
                itemsConfig.set(path + ".enchantments." + enchantName.toUpperCase(), entry.getValue());
            }
        }

        save();
    }

    public void save() {
        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save items.yml!");
            e.printStackTrace();
        }
    }

    public void reload() {
        loadItems();
    }

    public List<CustomItem> getPageItems(int page) {
        return pageItems.getOrDefault(page, new ArrayList<>());
    }

    public int getTotalPages() {
        return 7; // Updated from 3 to 7 pages
    }
}