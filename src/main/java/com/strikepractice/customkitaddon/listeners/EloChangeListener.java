package com.strikepractice.customkitaddon.listeners;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.events.DuelEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class EloChangeListener implements Listener {

    private final CustomKitAddon plugin;
    private final StrikePracticeAPI api;

    public EloChangeListener(CustomKitAddon plugin) {
        this.plugin = plugin;
        this.api = StrikePractice.getAPI();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDuelEnd(DuelEndEvent event) {
        // Check if fixated ELO is enabled
        if (!plugin.getConfigManager().isFixatedEloEnabled()) {
            return;
        }

        // Get winner and loser from event
        Player winner = event.getWinner();
        Player loser = event.getLoser();

        if (winner == null || loser == null) {
            return;
        }

        // Get the fixed ELO change amount from config
        int eloChange = plugin.getConfigManager().getEloChangeAmount();

        // Schedule the ELO change to happen after the event
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyFixedEloChange(winner, loser, eloChange);
        }, 5L); // 5 ticks delay to ensure StrikePractice has processed the match
    }

    private void applyFixedEloChange(Player winner, Player loser, int amount) {
        try {
            // Get the StrikePractice plugin instance
            Object spInstance = StrikePractice.getInstance();
            if (spInstance == null) {
                plugin.getLogger().warning("Could not get StrikePractice instance");
                return;
            }

            // Try to access the data/storage system
            Object dataManager = null;
            BattleKit lastKit = null;

            // Find the last kit used (we'll check all ELO-enabled kits)
            for (BattleKit kit : api.getKits()) {
                if (kit.isElo()) {
                    lastKit = kit;
                    break; // Get first ELO kit for now
                }
            }

            if (lastKit == null) {
                plugin.getLogger().warning("No ELO-enabled kit found");
                return;
            }

            // Try different approaches to modify ELO
            boolean success = false;

            // Approach 1: Try using PlayerData directly
            success = tryPlayerDataApproach(winner, loser, lastKit, amount);

            if (!success) {
                // Approach 2: Try using config files directly
                success = tryConfigFileApproach(winner, loser, lastKit.getName(), amount);
            }

            if (!success) {
                // Approach 3: Try using database if available
                success = tryDatabaseApproach(winner, loser, lastKit.getName(), amount);
            }

            if (success) {
                // Send messages to players
                winner.sendMessage("§a§l+" + amount + " ELO");
                loser.sendMessage("§c§l-" + amount + " ELO");

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Successfully applied fixed ELO changes");
                    plugin.getLogger().info("  Winner: " + winner.getName() + " (+" + amount + ")");
                    plugin.getLogger().info("  Loser: " + loser.getName() + " (-" + amount + ")");
                }
            } else {
                plugin.getLogger().warning("Failed to apply ELO changes - all approaches failed");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error in applyFixedEloChange: " + e.getMessage());
            if (plugin.getConfigManager().isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    private boolean tryPlayerDataApproach(Player winner, Player loser, BattleKit kit, int amount) {
        try {
            // Get the main StrikePractice instance
            StrikePractice sp = StrikePractice.getInstance();

            // Try to find the PlayerDataManager or similar
            Field[] fields = sp.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(sp);

                if (fieldValue == null) continue;

                String className = fieldValue.getClass().getSimpleName().toLowerCase();

                // Look for player data manager
                if (className.contains("player") && (className.contains("data") || className.contains("manager"))) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Found potential PlayerDataManager: " + field.getName());
                    }

                    // Try to get player data for winner and loser
                    Method getPlayerMethod = null;
                    for (Method method : fieldValue.getClass().getDeclaredMethods()) {
                        if (method.getParameterCount() == 1 &&
                                method.getParameterTypes()[0] == Player.class &&
                                (method.getName().toLowerCase().contains("get") || method.getName().toLowerCase().contains("load"))) {
                            getPlayerMethod = method;
                            break;
                        }
                    }

                    if (getPlayerMethod != null) {
                        getPlayerMethod.setAccessible(true);

                        Object winnerData = getPlayerMethod.invoke(fieldValue, winner);
                        Object loserData = getPlayerMethod.invoke(fieldValue, loser);

                        if (winnerData != null && loserData != null) {
                            // Now try to modify ELO in the player data
                            if (modifyEloInPlayerData(winnerData, kit.getName(), amount, true) &&
                                    modifyEloInPlayerData(loserData, kit.getName(), amount, false)) {

                                // Try to save the data
                                savePlayerData(winnerData);
                                savePlayerData(loserData);

                                return true;
                            }
                        }
                    }
                }

                // Also check for ELO-specific managers
                if (className.contains("elo") || className.contains("rating")) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Found potential ELO manager: " + field.getName());
                    }

                    // Try to use this to set ELO
                    if (trySetEloViaManager(fieldValue, winner, loser, kit.getName(), amount)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("PlayerData approach failed: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean modifyEloInPlayerData(Object playerData, String kitName, int amount, boolean isWinner) {
        try {
            // Look for ELO map or storage in player data
            Field[] fields = playerData.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(playerData);

                if (value instanceof Map) {
                    Map map = (Map) value;

                    // Check if this map contains ELO data
                    for (Object key : map.keySet()) {
                        if (key.toString().toLowerCase().contains("elo") ||
                                (key instanceof String && ((String)key).equalsIgnoreCase(kitName))) {

                            Object eloValue = map.get(key);
                            if (eloValue instanceof Integer) {
                                int currentElo = (Integer) eloValue;
                                int newElo = isWinner ? currentElo + amount : Math.max(0, currentElo - amount);
                                map.put(key, newElo);

                                if (plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().info("Modified ELO in map: " + currentElo + " -> " + newElo);
                                }
                                return true;
                            }
                        }
                    }
                }
            }

            // Also try direct methods
            for (Method method : playerData.getClass().getDeclaredMethods()) {
                if (method.getName().toLowerCase().contains("setelo")) {
                    method.setAccessible(true);

                    if (method.getParameterCount() == 2) {
                        // Likely setElo(String kit, int elo)
                        try {
                            // First get current ELO
                            int currentElo = 1000;
                            Method getMethod = playerData.getClass().getDeclaredMethod("getElo", String.class);
                            if (getMethod != null) {
                                getMethod.setAccessible(true);
                                currentElo = (int) getMethod.invoke(playerData, kitName);
                            }

                            int newElo = isWinner ? currentElo + amount : Math.max(0, currentElo - amount);
                            method.invoke(playerData, kitName, newElo);

                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("Set ELO via method: " + currentElo + " -> " + newElo);
                            }
                            return true;
                        } catch (Exception e) {
                            // Try next method
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Failed to modify ELO in player data: " + e.getMessage());
            }
        }
        return false;
    }

    private void savePlayerData(Object playerData) {
        try {
            // Try to find and call save method
            for (Method method : playerData.getClass().getDeclaredMethods()) {
                if (method.getName().toLowerCase().contains("save") && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    method.invoke(playerData);
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore save errors
        }
    }

    private boolean trySetEloViaManager(Object manager, Player winner, Player loser, String kitName, int amount) {
        try {
            // Look for setElo or updateElo methods
            for (Method method : manager.getClass().getDeclaredMethods()) {
                method.setAccessible(true);

                if (method.getName().toLowerCase().contains("setelo") ||
                        method.getName().toLowerCase().contains("updateelo")) {

                    // Try different parameter combinations
                    if (method.getParameterCount() == 3) {
                        // Possibly (Player, String kit, int elo) or (UUID, String, int)
                        try {
                            // Get current ELO first
                            int winnerElo = 1000;
                            int loserElo = 1000;

                            // Try to get current ELO
                            Method getMethod = manager.getClass().getDeclaredMethod("getElo", Player.class, String.class);
                            if (getMethod != null) {
                                getMethod.setAccessible(true);
                                winnerElo = (int) getMethod.invoke(manager, winner, kitName);
                                loserElo = (int) getMethod.invoke(manager, loser, kitName);
                            }

                            // Set new ELO
                            method.invoke(manager, winner, kitName, winnerElo + amount);
                            method.invoke(manager, loser, kitName, Math.max(0, loserElo - amount));

                            return true;
                        } catch (Exception e) {
                            // Try UUID version
                            try {
                                method.invoke(manager, winner.getUniqueId(), kitName, amount);
                                method.invoke(manager, loser.getUniqueId(), kitName, -amount);
                                return true;
                            } catch (Exception ex) {
                                // Continue trying
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Continue to next approach
        }
        return false;
    }

    private boolean tryConfigFileApproach(Player winner, Player loser, String kitName, int amount) {
        // This would involve directly modifying player data files
        // Implementation depends on how StrikePractice stores data
        // For now, return false as this is risky
        return false;
    }

    private boolean tryDatabaseApproach(Player winner, Player loser, String kitName, int amount) {
        // This would involve direct database queries
        // Implementation depends on StrikePractice's database setup
        // For now, return false
        return false;
    }
}