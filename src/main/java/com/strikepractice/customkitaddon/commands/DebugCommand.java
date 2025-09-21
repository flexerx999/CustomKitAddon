package com.strikepractice.customkitaddon.commands;

import com.strikepractice.customkitaddon.CustomKitAddon;
import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DebugCommand implements CommandExecutor {

    private final CustomKitAddon plugin;

    public DebugCommand(CustomKitAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customkit.admin")) {
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player");
            return true;
        }

        Player player = (Player) sender;
        sender.sendMessage("§e=== StrikePractice Debug Info ===");

        try {
            // Check API methods
            StrikePracticeAPI api = StrikePractice.getAPI();
            sender.sendMessage("§aAPI Methods:");
            for (Method method : api.getClass().getMethods()) {
                if (method.getName().toLowerCase().contains("elo") ||
                        method.getName().toLowerCase().contains("rating") ||
                        method.getName().toLowerCase().contains("stat")) {
                    sender.sendMessage("  §7- " + method.getName() + " (" + method.getParameterCount() + " params)");
                }
            }

            // Check PlayerKits for ELO methods
            Object playerKits = api.getPlayerKits(player);
            if (playerKits != null) {
                sender.sendMessage("§aPlayerKits Methods:");
                for (Method method : playerKits.getClass().getDeclaredMethods()) {
                    if (method.getName().toLowerCase().contains("elo")) {
                        sender.sendMessage("  §7- " + method.getName() + " (" + method.getParameterCount() + " params)");
                    }
                }

                sender.sendMessage("§aPlayerKits Fields:");
                for (Field field : playerKits.getClass().getDeclaredFields()) {
                    if (field.getName().toLowerCase().contains("elo") ||
                            field.getType() == java.util.Map.class) {
                        sender.sendMessage("  §7- " + field.getName() + " (" + field.getType().getSimpleName() + ")");
                    }
                }
            }

            // Check main plugin structure
            StrikePractice sp = StrikePractice.getInstance();
            sender.sendMessage("§aStrikePractice Fields:");
            for (Field field : sp.getClass().getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (name.contains("player") || name.contains("data") || name.contains("elo") || name.contains("manager")) {
                    sender.sendMessage("  §7- " + field.getName() + " (" + field.getType().getSimpleName() + ")");
                }
            }

        } catch (Exception e) {
            sender.sendMessage("§cError: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}