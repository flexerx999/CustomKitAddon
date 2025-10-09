package com.strikepractice.customkitaddon;

import com.strikepractice.customkitaddon.commands.AdminCommand;
import com.strikepractice.customkitaddon.commands.CustomKitCommand;
import com.strikepractice.customkitaddon.commands.DebugCommand;
import com.strikepractice.customkitaddon.config.ConfigManager;
import com.strikepractice.customkitaddon.config.ItemsConfig;
import com.strikepractice.customkitaddon.gui.GUIManager;
import com.strikepractice.customkitaddon.listeners.InventoryClickListener;
import com.strikepractice.customkitaddon.listeners.StrikePracticeListener;
import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.fights.elo.EloCalculator;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomKitAddon extends JavaPlugin {

    private static CustomKitAddon instance;
    private StrikePracticeAPI strikePracticeAPI;
    private ConfigManager configManager;
    private ItemsConfig itemsConfig;
    private GUIManager guiManager;
    private CustomKitCommand customKitCommand;
    private boolean eloCalculatorRegistered = false;

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

        // Register ELO calculator only if enabled
        updateEloCalculator();

        getLogger().info("CustomKitAddon v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Hooked into StrikePractice successfully!");

        // Enable debug mode for testing
        if (configManager.isDebugEnabled()) {
            getLogger().info("Debug mode is enabled - extra logging active");
        }
    }

    @Override
    public void onDisable() {
        // Unregister ELO calculator on disable
        if (eloCalculatorRegistered) {
            unregisterEloCalculator();
        }

        getLogger().info("CustomKitAddon has been disabled!");
        HandlerList.unregisterAll(this);
    }

    private void registerCommands() {
        // Admin command
        if (getCommand("customkitadmin") != null) {
            getCommand("customkitadmin").setExecutor(new AdminCommand(this));
        } else {
            getLogger().warning("Command 'customkitadmin' not found in plugin.yml");
        }

        // Debug command - only register if it exists in plugin.yml
        if (getCommand("ckdebug") != null) {
            getCommand("ckdebug").setExecutor(new DebugCommand(this));
        } else {
            getLogger().info("Debug command 'ckdebug' not registered (not in plugin.yml)");
        }

        // Register customkit interceptor
        customKitCommand = new CustomKitCommand(this);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StrikePracticeListener(this), this);
    }

    private void registerEloCalculator() {
        if (configManager.isFixatedEloEnabled() && !eloCalculatorRegistered) {
            EloCalculator.setEloCalculator(eloChanges -> {
                // Check if loser's current ELO is 0 or less
                if (eloChanges.getLoserOldElo() <= 0) {
                    // No changes for either player
                    eloChanges.setWinnerNewElo(eloChanges.getWinnerOldElo());
                    eloChanges.setLoserNewElo(eloChanges.getLoserOldElo());

                    if (getConfigManager().isDebugEnabled()) {
                        getLogger().info("ELO Change prevented: Loser already at 0 or below (was " +
                                eloChanges.getLoserOldElo() + ") - No changes applied");
                    }
                } else {
                    // Normal ELO changes
                    eloChanges.setWinnerNewElo(eloChanges.getWinnerOldElo() + getConfigManager().getEloChangeAmount());
                    eloChanges.setLoserNewElo(eloChanges.getLoserOldElo() - getConfigManager().getEloChangeAmount());
                }
            });

            eloCalculatorRegistered = true;
            getLogger().info("Fixated ELO Calculator REGISTERED - Changes set to: +" + configManager.getEloChangeAmount() + "/-" + configManager.getEloChangeAmount());
            getLogger().info("ELO Protection: Players at 0 or below ELO cannot lose more points");
        }
    }

    private void unregisterEloCalculator() {
        if (eloCalculatorRegistered) {
            // Reset to default calculator (null uses StrikePractice's default)
            EloCalculator.setEloCalculator(null);
            eloCalculatorRegistered = false;
            getLogger().info("Fixated ELO Calculator UNREGISTERED - Using default StrikePractice ELO calculation");
        }
    }

    private void updateEloCalculator() {
        if (configManager.isFixatedEloEnabled()) {
            // Register if not already registered
            if (!eloCalculatorRegistered) {
                registerEloCalculator();
            }
        } else {
            // Unregister if currently registered
            if (eloCalculatorRegistered) {
                unregisterEloCalculator();
            }
        }
    }

    public void reload() {
        // Reload configs
        configManager.reload();
        itemsConfig.reload();
        guiManager.clearCache();

        // Update ELO calculator based on new config
        updateEloCalculator();

        getLogger().info("Configuration reloaded!");

        // Log current ELO status
        if (configManager.isFixatedEloEnabled()) {
            getLogger().info("Fixated ELO is now ENABLED with change amount: " + configManager.getEloChangeAmount());
        } else {
            getLogger().info("Fixated ELO is now DISABLED - using default calculation");
        }
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

    public CustomKitCommand getCustomKitCommand() {
        return customKitCommand;
    }
}