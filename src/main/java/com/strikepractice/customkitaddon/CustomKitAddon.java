package com.strikepractice.customkitaddon;

import com.strikepractice.customkitaddon.commands.AdminCommand;
import com.strikepractice.customkitaddon.commands.CustomKitCommand;
import com.strikepractice.customkitaddon.commands.TestCommand;
import com.strikepractice.customkitaddon.config.ConfigManager;
import com.strikepractice.customkitaddon.config.ItemsConfig;
import com.strikepractice.customkitaddon.gui.GUIManager;
import com.strikepractice.customkitaddon.listeners.ChatListener;
import com.strikepractice.customkitaddon.listeners.InventoryClickListener;
import com.strikepractice.customkitaddon.listeners.StrikePracticeListener;
import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomKitAddon extends JavaPlugin {

    private static CustomKitAddon instance;
    private StrikePracticeAPI strikePracticeAPI;
    private ConfigManager configManager;
    private ItemsConfig itemsConfig;
    private GUIManager guiManager;
    private ChatListener chatListener;
    private CustomKitCommand customKitCommand;

    @Override
    public void onEnable() {
        instance = this;

        // Check for StrikePractice
        if (!Bukkit.getPluginManager().isPluginEnabled("StrikePractice")) {
            getLogger().severe("StrikePractice not found! Disabling CustomKitAddon...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize StrikePractice API
        strikePracticeAPI = StrikePractice.getAPI();

        // Create data folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize configs
        configManager = new ConfigManager(this);
        itemsConfig = new ItemsConfig(this);

        // Initialize GUI manager
        guiManager = new GUIManager(this);

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        getLogger().info("CustomKitAddon v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Hooked into StrikePractice successfully!");

        // Enable debug mode for testing
        if (configManager.isDebugEnabled()) {
            getLogger().info("Debug mode is enabled - extra logging active");
        }
    }

    @Override
    public void onDisable() {
        if (chatListener != null) {
            chatListener.cleanup();
        }
        getLogger().info("CustomKitAddon has been disabled!");
    }

    private void registerCommands() {
        // Admin command
        getCommand("customkitadmin").setExecutor(new AdminCommand(this));

        // Test/debug command
        getCommand("cktest").setExecutor(new TestCommand(this));

        // Register customkit interceptor
        customKitCommand = new CustomKitCommand(this);
    }

    private void registerListeners() {
        chatListener = new ChatListener(this);

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(chatListener, this);
        Bukkit.getPluginManager().registerEvents(new StrikePracticeListener(this), this);
    }

    public void reload() {
        configManager.reload();
        itemsConfig.reload();
        guiManager.clearCache();
        getLogger().info("Configuration reloaded!");
    }

    public static CustomKitAddon getInstance() {
        return instance;
    }

    public StrikePracticeAPI getStrikePracticeAPI() {
        return strikePracticeAPI;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemsConfig getItemsConfig() {
        return itemsConfig;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public CustomKitCommand getCustomKitCommand() {
        return customKitCommand;
    }
}