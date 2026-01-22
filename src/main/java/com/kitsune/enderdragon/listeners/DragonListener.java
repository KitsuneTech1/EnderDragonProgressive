package com.kitsune.enderdragon.listeners;

import com.kitsune.enderdragon.EnderDragonProgressive;
import com.kitsune.enderdragon.abilities.DragonAbilities;
import com.kitsune.enderdragon.items.ArtifactManager;
import com.kitsune.enderdragon.loot.LootTables;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class DragonListener implements Listener {

    private final EnderDragonProgressive plugin;
    private final ArtifactManager artifactManager;
    private final org.bukkit.NamespacedKey levelKey;
    private final Random random = new Random();
    private boolean roaredThisFight = false;
    private BossBar bossBar;

    public DragonListener(EnderDragonProgressive plugin, ArtifactManager artifactManager) {
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        this.levelKey = new org.bukkit.NamespacedKey(plugin, "dragon_level");
    }

    @EventHandler
    public void onDragonSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON)
            return;

        plugin.getLogger().info("=== DRAGON SPAWN EVENT ===");

        EnderDragon dragon = (EnderDragon) event.getEntity();
        int level = plugin.getConfig().getInt("next-dragon-level", 1);

        plugin.getLogger().info("Config next-dragon-level: " + level);

        // Mark the dragon with its level
        dragon.getPersistentDataContainer().set(levelKey, org.bukkit.persistence.PersistentDataType.INTEGER, level);
        plugin.getLogger().info("Tagged dragon with PDC level: " + level);

        // Scale Health
        double baseHealth = 200.0;
        double health;
        double lowScale = plugin.getConfig().getDouble("health-scaling-low", 1.075);
        double highScale = plugin.getConfig().getDouble("health-scaling-high", 1.01);

        if (level <= 20) {
            health = baseHealth * Math.pow(lowScale, level);
        } else {
            health = (baseHealth * Math.pow(lowScale, 20)) * Math.pow(highScale, level - 20);
        }

        dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
        dragon.setHealth(health);
        plugin.getLogger().info("Set dragon health to: " + health);

        // Fix for stationary dragon: Set AI phase
        dragon.setPhase(EnderDragon.Phase.CIRCLING);
        plugin.getLogger().info("Set dragon phase to CIRCLING");

        // Scale Damage (Implicitly via attribute if possible, or we manually handle
        // entity damage event)
        // Paper has GENERIC_ATTACK_DAMAGE for some entities, but Dragon often uses
        // phases.

        // Create and setup Custom Boss Bar
        if (bossBar != null)
            bossBar.removeAll();
        bossBar = Bukkit.createBossBar(
                "Level " + level + " Ender Dragon",
                BarColor.PURPLE,
                BarStyle.SOLID);
        for (org.bukkit.entity.Player p : dragon.getWorld().getPlayers()) {
            bossBar.addPlayer(p);
        }

        // Update Name Tag (5-tick delay to override vanilla name setting)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!dragon.isValid())
                    return;
                Component name = Component.text("Level " + level + " Ender Dragon", NamedTextColor.LIGHT_PURPLE,
                        TextDecoration.BOLD);
                dragon.customName(name);
                dragon.setCustomNameVisible(true);
            }
        }.runTaskLater(plugin, 5L);

        roaredThisFight = false;

        // Ability & UI Task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    this.cancel();
                    return;
                }

                // Sync Boss Bar
                if (bossBar != null) {
                    double progress = dragon.getHealth() / health;
                    bossBar.setProgress(Math.clamp(progress, 0.0, 1.0));
                    bossBar.setTitle("Level " + level + " Ender Dragon");
                    // Ensure all players in End see it
                    for (org.bukkit.entity.Player p : dragon.getWorld().getPlayers()) {
                        if (!bossBar.getPlayers().contains(p))
                            bossBar.addPlayer(p);
                    }
                }

                if (level >= 5 && random.nextDouble() < 0.1) {
                    DragonAbilities.spawnEndermiteSwarm(dragon);
                }

                if (level >= 20 && !roaredThisFight && dragon.getHealth() < (health / 2)) {
                    DragonAbilities.triggerRoar(dragon);
                    roaredThisFight = true;
                }
            }
        }.runTaskTimer(plugin, 100, 200); // Check every 10s
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON)
            return;

        plugin.getLogger().info("=== DRAGON DEATH EVENT ===");

        EnderDragon dragon = (EnderDragon) event.getEntity();

        // Retrieve the level from the dragon itself, or fallback to config if untagged
        int level;
        boolean hadPDC = dragon.getPersistentDataContainer().has(levelKey,
                org.bukkit.persistence.PersistentDataType.INTEGER);
        plugin.getLogger().info("Dragon has PDC level tag: " + hadPDC);

        if (hadPDC) {
            level = dragon.getPersistentDataContainer().get(levelKey,
                    org.bukkit.persistence.PersistentDataType.INTEGER);
            plugin.getLogger().info("Retrieved level from PDC: " + level);
        } else {
            // Fallback for transition: if the dragon was already there, subtract 1 from
            // "next" to get "current"
            int configLevel = plugin.getConfig().getInt("next-dragon-level", 1);
            level = Math.max(1, configLevel - 1);
            plugin.getLogger().info("No PDC tag. Config next-dragon-level: " + configLevel + ", using level: " + level);
        }

        int newLevel = level + 1;
        plugin.getLogger().info("Dragon Level " + level + " killed! Setting next-dragon-level to " + newLevel);

        plugin.getConfig().set("next-dragon-level", newLevel);
        plugin.getConfig().set("next-respawn-time",
                System.currentTimeMillis() + (plugin.getConfig().getInt("respawn-delay") * 1000L));

        plugin.getLogger().info("Calling saveConfig()...");
        plugin.saveConfig();
        plugin.getLogger().info("saveConfig() completed.");

        plugin.getLogger().info(
                "Verified next-dragon-level in memory: " + plugin.getConfig().getInt("next-dragon-level"));

        // Loot
        plugin.getLogger().info("Generating loot for level " + level + "...");
        List<ItemStack> loot = LootTables.getLootForLevel(level);
        Location loc = dragon.getLocation();

        plugin.getLogger().info("Dropping " + loot.size() + " loot items:");
        for (ItemStack item : loot) {
            plugin.getLogger().info("  - " + item.getType() + " x" + item.getAmount());
            loc.getWorld().dropItemNaturally(loc, item);
        }

        // Dragon Heart
        if (level % 3 == 0) {
            plugin.getLogger().info("Level " + level + " is divisible by 3 - dropping Dragon Heart!");
            loc.getWorld().dropItemNaturally(loc, artifactManager.createDragonHeart());
        } else {
            plugin.getLogger().info("Level " + level + " is NOT divisible by 3 - no Dragon Heart.");
        }

        // XP Distribution
        event.setDroppedExp(0);
        int totalXp = (int) LootTables.getExpForLevel(level, plugin.getConfig().getDouble("xp-scaling-factor", 0.1));
        plugin.getLogger().info("Dropping " + totalXp + " XP in 5 orbs");
        int orbs = 5;
        for (int i = 0; i < orbs; i++) {
            ExperienceOrb orb = (ExperienceOrb) loc.getWorld().spawnEntity(loc, EntityType.EXPERIENCE_ORB);
            orb.setExperience(totalXp / orbs);
        }

        // Cleanup Boss Bar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
            plugin.getLogger().info("Boss bar removed.");
        }

        plugin.getLogger().info("=== DRAGON DEATH EVENT COMPLETE ===");

        // Announcement
        Component msg = Component
                .text("The Level " + level + " Ender Dragon has been slain!", NamedTextColor.GOLD,
                        TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("Loot dropped: Elytra, Spawners, Totems, and massive treasures!",
                        NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("A new dragon will awaken in 10 minutes!", NamedTextColor.DARK_PURPLE));
        Bukkit.broadcast(msg);

        // Respawn Timer
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getConfig().set("next-respawn-time", 0);
                plugin.saveConfig();

                Bukkit.broadcast(
                        Component.text("A new Ender Dragon is beginning to awaken...", NamedTextColor.DARK_PURPLE));

                // Only use summon command (initiateRespawn can cause double spawns)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "execute in minecraft:the_end run summon ender_dragon 0 80 0");
            }
        }.runTaskLater(plugin, plugin.getConfig().getInt("respawn-delay") * 20L);
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.DRAGON_FIREBALL) {
            // Since fireballs don't easily store the level, we check the current config
            // level
            // but safely default to a high value if needed, or just use next-dragon-level -
            // 1
            int level = plugin.getConfig().getInt("next-dragon-level", 1) - 1;
            if (level >= 10) {
                DragonAbilities.handlePurpleFire(event.getLocation());
            }
        }
    }

    @EventHandler
    public void onHeartConsume(PlayerInteractEvent event) {
        if (event.getItem() != null) {
            artifactManager.handleConsumption(event.getPlayer(), event.getItem());
        }
    }

    @EventHandler
    public void onDragonDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EnderDragon dragon) {
            int level = dragon.getPersistentDataContainer().getOrDefault(levelKey,
                    org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            double lowScale = plugin.getConfig().getDouble("damage-scaling-low", 1.02);
            double highScale = plugin.getConfig().getDouble("damage-scaling-high", 1.005);

            double multiplier;
            if (level <= 20) {
                multiplier = Math.pow(lowScale, level);
            } else {
                multiplier = Math.pow(lowScale, 20) * Math.pow(highScale, level - 20);
            }

            event.setDamage(event.getDamage() * multiplier);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (bossBar == null)
            return;
        if (event.getPlayer().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            bossBar.addPlayer(event.getPlayer());
        } else {
            bossBar.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (bossBar == null)
            return;
        if (event.getTo().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            bossBar.addPlayer(event.getPlayer());
        } else {
            bossBar.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        artifactManager.applyHealthBonus(event.getPlayer());
        if (bossBar != null && event.getPlayer().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            bossBar.addPlayer(event.getPlayer());
        }
    }
}
