package com.kitsune.enderdragon.commands;

import com.kitsune.enderdragon.EnderDragonProgressive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DragonCommand implements CommandExecutor {

    private final EnderDragonProgressive plugin;

    public DragonCommand(EnderDragonProgressive plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("enderdragon.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /dragon <reset|setlevel> [value]", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            plugin.getConfig().set("next-dragon-level", 1);
            plugin.saveConfig();
            sender.sendMessage(Component.text("Dragon level has been reset to 1.", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("kill")) {
            org.bukkit.World endWorld = org.bukkit.Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == org.bukkit.World.Environment.THE_END)
                    .findFirst().orElse(null);

            if (endWorld == null) {
                sender.sendMessage(Component.text("End world not found!", NamedTextColor.RED));
                return true;
            }

            int count = 0;
            for (org.bukkit.entity.EnderDragon dragon : endWorld
                    .getEntitiesByClass(org.bukkit.entity.EnderDragon.class)) {
                dragon.remove();
                count++;
            }

            Component msg = Component.text("Removed " + count + " dragons from the End.", NamedTextColor.GREEN);
            sender.sendMessage(msg);

            // Notify all ops
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.isOp() && player != sender) {
                    player.sendMessage(msg);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                    "execute in minecraft:the_end run summon ender_dragon 0 80 0");
            sender.sendMessage(Component.text("A new legal dragon has been spawned.", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("setlevel") && args.length == 2) {
            try {
                int level = Integer.parseInt(args[1]);
                plugin.getConfig().set("next-dragon-level", level);
                plugin.saveConfig();
                sender.sendMessage(Component.text("Next dragon level set to " + level + ".", NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid level number.", NamedTextColor.RED));
            }
            return true;
        }

        return false;
    }
}
