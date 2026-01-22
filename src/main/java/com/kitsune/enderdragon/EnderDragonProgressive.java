package com.kitsune.enderdragon;

import com.kitsune.enderdragon.items.ArtifactManager;
import com.kitsune.enderdragon.listeners.DragonListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderDragonProgressive extends JavaPlugin {

    private ArtifactManager artifactManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.artifactManager = new ArtifactManager(this);

        getServer().getPluginManager().registerEvents(new DragonListener(this, artifactManager), this);
        getCommand("dragon").setExecutor(new com.kitsune.enderdragon.commands.DragonCommand(this));

        // Register listener for world loading
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onWorldLoad(WorldLoadEvent event) {
                if (event.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
                    getLogger().info("End world loaded. Checking dragon status...");
                    checkAndScheduleRespawn();
                }
            }
        }, this);

        // Start checking for dragon status after a short delay to allow worlds to load
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                checkAndScheduleRespawn();
            }
        }.runTaskLater(this, 100L); // 5 second delay

        getLogger().info("EnderDragonProgressive has been enabled!");
    }

    public void checkAndScheduleRespawn() {
        org.bukkit.World endWorld = org.bukkit.Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == org.bukkit.World.Environment.THE_END)
                .findFirst().orElse(null);

        if (endWorld == null) {
            getLogger().info("End world not loaded yet. Spawning check will trigger when the world loads.");
            return;
        }

        // Migrate old config if it exists
        if (getConfig().contains("dragon-level") && getConfig().getInt("next-dragon-level") == 1) {
            int oldLevel = getConfig().getInt("dragon-level");
            if (oldLevel > 0) {
                getConfig().set("next-dragon-level", oldLevel);
                // We keep the old key for one boot just in case, or we can remove it
                getLogger().info("Migrated old 'dragon-level' value (" + oldLevel + ") to 'next-dragon-level'.");
                saveConfig();
            }
        }

        getLogger().info("Checking dragon status. Next level: " + getConfig().getInt("next-dragon-level"));

        // Auto-detect previous kills for new installs
        if (getConfig().getInt("next-dragon-level") <= 1) {
            // Check if there is NO dragon but it HAS been killed before
            if (endWorld.getEnderDragonBattle() != null && endWorld.getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                if (getConfig().getInt("next-dragon-level") == 1 || getConfig().getInt("next-dragon-level") == 0) {
                    getConfig().set("next-dragon-level", 2);
                    saveConfig();
                    getLogger().info("Detected previous vanilla kill. Next dragon set to Level 2.");
                }
            } else if (getConfig().getInt("next-dragon-level") == 0) {
                // If it's a completely fresh world, start with level 1
                getConfig().set("next-dragon-level", 1);
                saveConfig();
            }
        }

        // If no dragon exists, spawn one immediately
        if (endWorld.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).isEmpty()) {
            getLogger().info("No active dragon found on startup. Spawning one immediately...");

            getConfig().set("next-respawn-time", 0);
            saveConfig();

            // Do NOT call initiateRespawn() here if we want an immediate summon
            // just use the command to spawn it
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                    "execute in minecraft:the_end run summon ender_dragon 0 80 0");
            org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text("The Ender Dragon has returned!",
                    net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE));
        } else {
            getLogger().info("Ender Dragon is already active.");
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("EnderDragonProgressive has been disabled!");
    }

    public ArtifactManager getArtifactManager() {
        return artifactManager;
    }
}
