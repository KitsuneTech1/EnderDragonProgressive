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
