package com.strikepractice.customkitaddon.config;

import com.strikepractice.customkitaddon.CustomKitAddon;
import com.strikepractice.customkitaddon.models.CustomItem;
import com.strikepractice.customkitaddon.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            int slot = section.getInt("slot", -1);
            boolean hasEnchantGui = section.getBoolean("enchantgui", false);

            // Check for full serialized ItemStack data first
            if (section.contains("data_type") && section.getString("data_type").equals("default")) {
                if (section.contains("data")) {
                    // Load serialized ItemStack
                    ConfigurationSection dataSection = section.getConfigurationSection("data");
                    if (dataSection != null) {
                        // Use Bukkit's built-in deserialization
                        Map<String, Object> data = dataSection.getValues(false);
                        ItemStack item = ItemStack.deserialize(data);

                        if (item != null && item.getType() != Material.AIR) {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("Loaded serialized item: " + item.getType());
                            }
                            CustomItem customItem = new CustomItem(item, slot);
                            customItem.setEnchantGui(hasEnchantGui);
                            return customItem;
                        }
                    }
                }
            }

            // Fallback to simple format loading
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

            // ItemFlags support
            if (section.contains("item-flags")) {
                List<String> flags = section.getStringList("item-flags");
                for (String flagName : flags) {
                    try {
                        ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                        builder.addItemFlag(flag);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid item flag: " + flagName);
                    }
                }
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

            // PublicBukkitValues / PersistentDataContainer support (1.14+)
            if (section.contains("custom-data")) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    try {
                        // Use reflection to support multiple versions
                        ConfigurationSection customData = section.getConfigurationSection("custom-data");
                        if (customData != null) {
                            applyCustomData(meta, customData);
                        }
                        item.setItemMeta(meta);
                    } catch (Exception e) {
                        // PDC not available in this version, ignore
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("PersistentDataContainer not available: " + e.getMessage());
                        }
                    }
                }
            }

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

            CustomItem customItem = new CustomItem(item, slot);
            customItem.setEnchantGui(hasEnchantGui);
            return customItem;

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading item from config: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void applyCustomData(ItemMeta meta, ConfigurationSection customData) {
        try {
            // Get PersistentDataContainer via reflection for version compatibility
            java.lang.reflect.Method getPDC = meta.getClass().getMethod("getPersistentDataContainer");
            Object pdc = getPDC.invoke(meta);

            // Get the NamespacedKey class
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");

            // Get PersistentDataType class and common types
            Class<?> pdtClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object STRING_TYPE = pdtClass.getField("STRING").get(null);
            Object INTEGER_TYPE = pdtClass.getField("INTEGER").get(null);
            Object DOUBLE_TYPE = pdtClass.getField("DOUBLE").get(null);
            Object BOOLEAN_TYPE = pdtClass.getField("BOOLEAN").get(null);

            // Get the set method
            java.lang.reflect.Method setMethod = pdc.getClass().getMethod("set",
                    namespacedKeyClass, pdtClass, Object.class);

            // Process each custom data entry
            for (String key : customData.getKeys(false)) {
                try {
                    // Parse the key for namespace
                    String namespace = "customkit";
                    String actualKey = key;

                    if (key.contains(":")) {
                        String[] parts = key.split(":", 2);
                        namespace = parts[0];
                        actualKey = parts[1];
                    }

                    // Create NamespacedKey
                    Object nsKey = namespacedKeyClass.getConstructor(String.class, String.class)
                            .newInstance(namespace, actualKey);

                    // Set value based on type
                    Object value = customData.get(key);
                    if (value instanceof String) {
                        setMethod.invoke(pdc, nsKey, STRING_TYPE, value);
                    } else if (value instanceof Integer) {
                        setMethod.invoke(pdc, nsKey, INTEGER_TYPE, value);
                    } else if (value instanceof Double) {
                        setMethod.invoke(pdc, nsKey, DOUBLE_TYPE, value);
                    } else if (value instanceof Boolean) {
                        setMethod.invoke(pdc, nsKey, BOOLEAN_TYPE, value);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to set custom data for key: " + key);
                }
            }
        } catch (Exception e) {
            // PDC not available, ignore
        }
    }

    public void addItem(int page, ItemStack item, int slot) {
        // First, remove any existing item at this slot
        removeItemAtSlot(page, slot);

        // Now add the new item
        List<CustomItem> items = pageItems.computeIfAbsent(page, k -> new ArrayList<>());
        items.add(new CustomItem(item, slot));

        // Save to config (this will also handle the replacement in the file)
        savePageToConfig(page);
    }

    private void removeItemAtSlot(int page, int slot) {
        List<CustomItem> items = pageItems.get(page);
        if (items != null) {
            items.removeIf(item -> item.getSlot() == slot);
        }

        // Also remove from config file
        ConfigurationSection pageSection = itemsConfig.getConfigurationSection("items.page" + page);
        if (pageSection != null) {
            // Find and remove any item with matching slot
            for (String key : new ArrayList<>(pageSection.getKeys(false))) {
                ConfigurationSection itemSection = pageSection.getConfigurationSection(key);
                if (itemSection != null && itemSection.getInt("slot", -1) == slot) {
                    pageSection.set(key, null);
                }
            }
        }
    }

    private void savePageToConfig(int page) {
        // Clear the page section
        itemsConfig.set("items.page" + page, null);

        // Save all items for this page
        List<CustomItem> items = pageItems.get(page);
        if (items != null) {
            int index = 0;
            for (CustomItem customItem : items) {
                String path = "items.page" + page + ".item" + index;
                saveItemToConfig(path, customItem.getItemStack(), customItem.getSlot());

                // Save enchantgui flag if present
                if (customItem.hasEnchantGui()) {
                    itemsConfig.set(path + ".enchantgui", true);
                }

                index++;
            }
        }

        save();
    }

    private void saveItemToConfig(String path, ItemStack item, int slot) {
        // Always save slot
        itemsConfig.set(path + ".slot", slot);

        // Try to save as serialized data (preserves ALL item data including PDC)
        try {
            // Serialize the entire ItemStack
            Map<String, Object> serialized = item.serialize();

            // Save as full serialized format
            itemsConfig.set(path + ".data_type", "default");
            itemsConfig.set(path + ".data", serialized);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Saved item as serialized data: " + item.getType());
            }

        } catch (Exception e) {
            // Fallback to simple format if serialization fails
            plugin.getLogger().warning("Failed to serialize item, using simple format: " + e.getMessage());
            saveSimpleFormat(path, item);
        }
    }

    private void saveSimpleFormat(String path, ItemStack item) {
        itemsConfig.set(path + ".material", item.getType().name());
        itemsConfig.set(path + ".amount", item.getAmount());

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

            // Save ItemFlags
            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) {
                    flags.add(flag.name());
                }
                itemsConfig.set(path + ".item-flags", flags);
            }

            // Save custom model data if present
            try {
                if (meta.hasCustomModelData()) {
                    itemsConfig.set(path + ".custom-model-data", meta.getCustomModelData());
                }
            } catch (NoSuchMethodError e) {
                // Method not available in this version
            }
        }

        if (!item.getEnchantments().isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                String enchantName = entry.getKey().getKey().getKey();
                itemsConfig.set(path + ".enchantments." + enchantName.toUpperCase(), entry.getValue());
            }
        }
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
        // Get from ConfigManager instead of hardcoding
        return plugin.getConfigManager().getTotalPages();
    }
}