package com.kitsune.enderdragon.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class DragonAbilities {

    private static final Random RANDOM = new Random();

    public static void spawnEndermiteSwarm(EnderDragon dragon) {
        Location loc = dragon.getLocation();
        for (int i = 0; i < 3 + RANDOM.nextInt(3); i++) {
            loc.getWorld().spawnEntity(loc.clone().add(RANDOM.nextInt(3) - 1, -1, RANDOM.nextInt(3) - 1),
                    EntityType.ENDERMITE);
        }
    }

    public static void triggerRoar(EnderDragon dragon) {
        dragon.getWorld().playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 5f, 0.5f);
        dragon.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, dragon.getLocation(), 10, 2, 2, 2);

        for (Player player : dragon.getWorld().getPlayers()) {
            if (player.getLocation().distance(dragon.getLocation()) < 50) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));
            }
        }
    }

    public static void handlePurpleFire(Location location) {
        // We can simulate purple fire using particles and a damaging task
        location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, 50, 1, 1, 1, 0.1);
        location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);

        // Damage nearby players
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) < 3) {
                player.damage(4.0);
            }
        }
    }
}
