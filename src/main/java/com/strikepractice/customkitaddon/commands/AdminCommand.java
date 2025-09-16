package com.strikepractice.customkitaddon.commands;

import com.strikepractice.customkitaddon.CustomKitAddon;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final CustomKitAddon plugin;

    public AdminCommand(CustomKitAddon plugin) {
        this.plugin = plugin;
        plugin.getCommand("customkitadmin").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customkit.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "add":
                handleAdd(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sendHelp(sender, label);
                break;
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (!sender.hasPermission("customkit.admin.add")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cYou must be holding an item to add!");
            return;
        }

        // Parse page number
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                page = Math.max(1, Math.min(page, 3)); // Clamp to 1-3
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid page number! Use 1-3.");
                return;
            }
        }

        // Parse slot number (optional)
        int slot = -1;
        if (args.length > 2) {
            try {
                slot = Integer.parseInt(args[2]);
                if (slot < 0 || slot > 44) {
                    player.sendMessage("§cSlot must be between 0-44!");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid slot number!");
                return;
            }
        }

        // Add the item
        plugin.getItemsConfig().addItem(page, itemInHand, slot);

        String message = "§aSuccessfully added " + itemInHand.getType().name() + " to page " + page;
        if (slot >= 0) {
            message += " at slot " + slot;
        }
        player.sendMessage(message);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("customkit.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        plugin.reload();
        sender.sendMessage("§aCustomKitAddon configuration reloaded!");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§7§m---------§r §b§lCustomKit Admin §7§m---------");
        sender.sendMessage("§e/" + label + " add [page] [slot] §7- Add held item to a page");
        sender.sendMessage("§e/" + label + " reload §7- Reload configuration");
        sender.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("customkit.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("add", "reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return Arrays.asList("1", "2", "3");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            List<String> slots = new ArrayList<>();
            for (int i = 0; i <= 44; i++) {
                slots.add(String.valueOf(i));
            }
            return slots;
        }

        return new ArrayList<>();
    }
}