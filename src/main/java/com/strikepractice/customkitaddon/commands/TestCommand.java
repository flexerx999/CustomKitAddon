package com.strikepractice.customkitaddon.commands;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.playerkits.PlayerKits;
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
        private void showDetailedKitInfo(Player player) {
            try {
                StrikePracticeAPI api = plugin.getStrikePracticeAPI();
                PlayerKits playerKits = api.getPlayerKits(player);
                BattleKit customKit = playerKits.getCustomKit();

                if (customKit == null) {
                    player.sendMessage("§cNo custom kit found!");
                    return;
                }

                player.sendMessage("§e=== Detailed Kit Info ===");

                // Show all fields
                for (java.lang.reflect.Field field : customKit.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(customKit);
                        if (value != null && (field.getName().toLowerCase().contains("name") ||
                                field.getName().toLowerCase().contains("display"))) {
                            player.sendMessage("§7" + field.getName() + ": §f" + value);
                        }
                    } catch (Exception ignored) {}
                }

                // Show all methods that might get the name
                player.sendMessage("§7Available getter methods:");
                for (java.lang.reflect.Method method : customKit.getClass().getMethods()) {
                    if ((method.getName().startsWith("get") && method.getName().toLowerCase().contains("name")) ||
                            method.getName().toLowerCase().contains("display")) {
                        if (method.getParameterCount() == 0) {
                            try {
                                Object result = method.invoke(customKit);
                                player.sendMessage("  §7" + method.getName() + "(): §f" + result);
                            } catch (Exception ignored) {}
                        }
                    }
                }

            } catch (Exception e) {
                player.sendMessage("§cError: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
            player.sendMessage("§7/cktest kit - Show your current custom kit info");
            player.sendMessage("§7/cktest gui - Test opening custom items GUI");
            player.sendMessage("§7/cktest api - Test StrikePractice API connection");
            player.sendMessage("§7/cktest rename <name> - Test kit renaming");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "kit":
                testKitInfo(player);
                break;
            case "gui":
                // Test with inventory slot 0 (which maps to GUI slot 18)
                plugin.getGuiManager().openCustomItemsGUI(player, 1, 0);
                player.sendMessage("§aOpening custom items GUI for inventory slot 0 (GUI slot 18)...");
                break;
            case "api":
                testAPI(player);
                break;
            case "rename":
                if (args.length > 1) {
                    String name = String.join(" ", args).substring(7);
                    testRename(player, name);
                } else {
                    player.sendMessage("§cUsage: /cktest rename <new name>");
                }
                break;
            case "kitinfo":
                showDetailedKitInfo(player);
                break;
            default:
                player.sendMessage("§cUnknown subcommand!");
        }

        return true;
    }

    private void testKitInfo(Player player) {
        try {
            StrikePracticeAPI api = plugin.getStrikePracticeAPI();
            PlayerKits playerKits = api.getPlayerKits(player);

            player.sendMessage("§e=== Custom Kit Test ===");

            // Get custom kit
            BattleKit customKit = playerKits.getCustomKit();

            if (customKit != null) {
                player.sendMessage("§aCustom kit found!");

                // Try to get kit name
                try {
                    java.lang.reflect.Field nameField = customKit.getClass().getDeclaredField("displayName");
                    nameField.setAccessible(true);
                    Object displayName = nameField.get(customKit);
                    player.sendMessage("§7Display Name: §f" + displayName);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Field nameField = customKit.getClass().getDeclaredField("name");
                        nameField.setAccessible(true);
                        Object name = nameField.get(customKit);
                        player.sendMessage("§7Name: §f" + name);
                    } catch (Exception ex) {
                        player.sendMessage("§7Name: §c(Could not retrieve)");
                    }
                }

                // Show inventory
                List<ItemStack> inventory = customKit.getInventory();
                player.sendMessage("§7Inventory size: §f" + inventory.size() + " slots");

                // Show items in inventory slots 0-35 (which correspond to GUI slots 18-53)
                player.sendMessage("§7Items (inventory slots 0-35, GUI slots 18-53):");
                for (int i = 0; i <= 35 && i < inventory.size(); i++) {
                    ItemStack item = inventory.get(i);
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        int guiSlot = i + 18;
                        player.sendMessage("  §7Slot " + i + " (GUI " + guiSlot + "): §f" + item.getType());
                    }
                }

            } else {
                player.sendMessage("§cNo custom kit found!");
                player.sendMessage("§7Open /customkit and select/create a kit first.");
            }

        } catch (Exception e) {
            player.sendMessage("§cError: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testAPI(Player player) {
        player.sendMessage("§e=== StrikePractice API Test ===");

        try {
            StrikePracticeAPI api = plugin.getStrikePracticeAPI();

            if (api != null) {
                player.sendMessage("§aAPI is connected!");

                // Test PlayerKits
                PlayerKits playerKits = api.getPlayerKits(player);
                if (playerKits != null) {
                    player.sendMessage("§aPlayerKits object retrieved!");

                    // Test getCustomKit
                    BattleKit customKit = playerKits.getCustomKit();
                    if (customKit != null) {
                        player.sendMessage("§aCustom kit exists!");
                    } else {
                        player.sendMessage("§7No custom kit found (this is normal if you haven't created one)");
                    }
                } else {
                    player.sendMessage("§cPlayerKits is null!");
                }

            } else {
                player.sendMessage("§cAPI is null!");
            }
        } catch (Exception e) {
            player.sendMessage("§cAPI Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testRename(Player player, String newName) {
        try {
            StrikePracticeAPI api = plugin.getStrikePracticeAPI();
            PlayerKits playerKits = api.getPlayerKits(player);
            BattleKit customKit = playerKits.getCustomKit();

            if (customKit == null) {
                player.sendMessage("§cNo custom kit found to rename!");
                return;
            }

            // Try to rename
            boolean success = false;
            try {
                java.lang.reflect.Field nameField = customKit.getClass().getDeclaredField("displayName");
                nameField.setAccessible(true);
                nameField.set(customKit, newName);
                success = true;
            } catch (NoSuchFieldException e) {
                try {
                    java.lang.reflect.Field nameField = customKit.getClass().getDeclaredField("name");
                    nameField.setAccessible(true);
                    nameField.set(customKit, newName);
                    success = true;
                } catch (Exception ex) {
                    player.sendMessage("§cCould not find name field: " + ex.getMessage());
                }
            }

            if (success) {
                player.sendMessage("§aKit renamed to: §f" + newName);
                player.sendMessage("§7Reopen /customkit to see the change.");
            }

        } catch (Exception e) {
            player.sendMessage("§cError: " + e.getMessage());
            e.printStackTrace();
        }
    }
}