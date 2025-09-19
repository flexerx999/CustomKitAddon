package com.strikepractice.customkitaddon.config;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final CustomKitAddon plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(CustomKitAddon plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        loadConfig();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml!");
            e.printStackTrace();
        }
    }

    // Getters for config values
    public String getMessage(String path) {
        return config.getString("messages." + path, "&cMissing message: " + path)
                .replace("&", "§");
    }

    public String getGuiTitle(int page) {
        return config.getString("gui.title", "&8Custom Kit Items - Page %page%")
                .replace("%page%", String.valueOf(page))
                .replace("&", "§");
    }

    public int getItemsPerPage() {
        return config.getInt("gui.items-per-page", 45);
    }

    public int getTotalPages() {
        int pages = config.getInt("gui.pages", 7); // Default to 7 if not set
        // Validate range (1-99)
        if (pages < 1) pages = 7;
        if (pages > 99) pages = 7;
        return pages;
    }

    public boolean isSoundEnabled() {
        return config.getBoolean("settings.sounds.enabled", true);
    }

    public String getSelectSound() {
        return config.getString("settings.sounds.select", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    public int getRenameTimeout() {
        return config.getInt("settings.rename-timeout", 30);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}