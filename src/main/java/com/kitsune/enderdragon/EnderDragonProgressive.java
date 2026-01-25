package com.kitsune.enderdragon;

import com.kitsune.enderdragon.items.ArtifactManager;
import com.kitsune.enderdragon.listeners.DragonListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderDragonProgressive extends JavaPlugin {

    private ArtifactManager artifactManager;
    private boolean isSpawnInProgress = false;

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
        if (isSpawnInProgress) {
            return;
        }

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
                getLogger().info("Migrated old 'dragon-level' value (" + oldLevel + ") to 'next-dragon-level'.");
                saveConfig();
            }
        }

        int nextLevel = getConfig().getInt("next-dragon-level");
        getLogger().info("Checking dragon status. Next level in config: " + nextLevel);

        // Auto-detect previous kills for new installs
        if (nextLevel <= 1) {
            if (endWorld.getEnderDragonBattle() != null && endWorld.getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                getConfig().set("next-dragon-level", 2);
                saveConfig();
                getLogger().info("Detected previous vanilla kill. Next dragon set to Level 2.");
            } else if (nextLevel == 0) {
                getConfig().set("next-dragon-level", 1);
                saveConfig();
            }
        }

        // Robust Detection & Automatic Cleanup
        java.util.Collection<org.bukkit.entity.EnderDragon> dragons = endWorld
                .getEntitiesByClass(org.bukkit.entity.EnderDragon.class);
        boolean battleDragonExists = endWorld.getEnderDragonBattle() != null
                && endWorld.getEnderDragonBattle().getEnderDragon() != null;

        getLogger().info("Detection: Found " + dragons.size() + " EnderDragon entities.");
        getLogger().info("Detection: EnderDragonBattle dragon present: " + battleDragonExists);

        if (dragons.size() > 1) {
            getLogger().warning("CRITICAL: Found " + dragons.size() + " dragons! Cleaning up extra dragons...");
            for (org.bukkit.entity.EnderDragon d : dragons) {
                d.remove();
            }
            // Trigger a single fresh spawn
            spawnLegalDragon();
        } else if (dragons.isEmpty() && !battleDragonExists) {
            getLogger().info("No active dragon found. Spawning one...");
            spawnLegalDragon();
        } else {
            getLogger().info("Ender Dragon is already active (1 dragon found). Skipping spawn.");
        }
    }

    private void spawnLegalDragon() {
        isSpawnInProgress = true;
        getConfig().set("next-respawn-time", 0);
        saveConfig();

        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                "execute in minecraft:the_end run summon ender_dragon 0 80 0");
        org.bukkit.Bukkit
                .broadcast(net.kyori.adventure.text.Component.text("A single, legal Ender Dragon has been summoned!",
                        net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE));

        // Reset guard after 1 second to allow entity to register
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                isSpawnInProgress = false;
            }
        }.runTaskLater(this, 20L);
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
