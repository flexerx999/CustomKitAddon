package com.strikepractice.customkitaddon.commands;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.battlekit.BattleKit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TestCommand implements CommandExecutor {

    private final CustomKitAddon plugin;

    public TestCommand(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§e=== CustomKitAddon Debug Info ===");
            player.sendMessage("§7/cktest kit - Show your current kit info");
            player.sendMessage("§7/cktest gui - Test opening custom GUI");
            player.sendMessage("§7/cktest api - Test StrikePractice API");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "kit":
                testKitInfo(player);
                break;
            case "gui":
                plugin.getGuiManager().openCustomItemsGUI(player, 1, 0);
                player.sendMessage("§aOpening custom items GUI...");
                break;
            case "api":
                testAPI(player);
                break;
            default:
                player.sendMessage("§cUnknown subcommand!");
        }

        return true;
    }

    private void testKitInfo(Player player) {
        try {
            ga.strikepractice.api.StrikePracticeAPI api = plugin.getStrikePracticeAPI();

            player.sendMessage("§e=== Kit Test ===");

            // Test getEditingKit()
            ga.strikepractice.battlekit.BattleKit editingKit = api.getEditingKit(player);
            if (editingKit != null) {
                player.sendMessage("§aEditing kit found!");

                // Check all fields
                player.sendMessage("§7Kit fields:");
                for (java.lang.reflect.Field field : editingKit.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(editingKit);
                        player.sendMessage("§7- " + field.getName() + " = " + value);
                    } catch (Exception ignored) {}
                }
            } else {
                player.sendMessage("§cNo editing kit found");
            }

            // Test getKit(player)
            ga.strikepractice.battlekit.BattleKit kit = api.getKit(player);
            if (kit != null) {
                player.sendMessage("§aPlayer kit found!");
            }

        } catch (Exception e) {
            player.sendMessage("§cError: " + e.getMessage());
        }
    }

    private void testAPI(Player player) {
        player.sendMessage("§e=== StrikePractice API Test ===");

        try {
            // Test if API is available
            if (plugin.getStrikePracticeAPI() != null) {
                player.sendMessage("§aAPI is available!");

                // Test fight detection
                if (plugin.getStrikePracticeAPI().getFight(player) != null) {
                    player.sendMessage("§7You are in a fight");
                } else {
                    player.sendMessage("§7You are not in a fight");
                }

                // Test kit retrieval
                BattleKit kit = plugin.getStrikePracticeAPI().getKit(player);
                if (kit != null) {
                    player.sendMessage("§7Kit retrieved successfully");
                } else {
                    player.sendMessage("§7No kit found");
                }
            } else {
                player.sendMessage("§cAPI is null!");
            }
        } catch (Exception e) {
            player.sendMessage("§cAPI Error: " + e.getMessage());
        }
    }
}