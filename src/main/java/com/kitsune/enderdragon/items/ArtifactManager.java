package com.kitsune.enderdragon.items;

import com.kitsune.enderdragon.EnderDragonProgressive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArtifactManager {

    private final EnderDragonProgressive plugin;
    private final NamespacedKey heartKey;

    public ArtifactManager(EnderDragonProgressive plugin) {
        this.plugin = plugin;
        this.heartKey = new NamespacedKey(plugin, "dragon_heart");
    }

    public ItemStack createDragonHeart() {
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();

        meta.displayName(Component.text("Dragon Heart", NamedTextColor.RED, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Consume to gain 2 Permanent Hearts!", NamedTextColor.GRAY));
        lore.add(Component.text("Drops every 3 levels of the Ender Dragon.", NamedTextColor.DARK_PURPLE));
        meta.lore(lore);

        // Mark the item
        meta.getPersistentDataContainer().set(heartKey, PersistentDataType.BYTE, (byte) 1);

        heart.setItemMeta(meta);
        return heart;
    }

    public boolean isDragonHeart(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(heartKey, PersistentDataType.BYTE);
    }

    public void applyHealthBonus(Player player) {
        UUID uuid = player.getUniqueId();
        double bonus = plugin.getConfig().getDouble("player-health-bonuses." + uuid, 0.0);

        AttributeInstance hp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(20.0 + bonus);
        }
    }

    public void handleConsumption(Player player, ItemStack item) {
        if (!isDragonHeart(item))
            return;

        double currentBonus = plugin.getConfig().getDouble("player-health-bonuses." + player.getUniqueId(), 0.0);
        double maxCap = plugin.getConfig().getDouble("max-player-health-cap", 100.0);

        if (currentBonus >= maxCap) {
            player.sendMessage(
                    Component.text("You have already reached the maximum health bonus!", NamedTextColor.RED));
            return;
        }

        item.setAmount(item.getAmount() - 1);
        double newBonus = currentBonus + 4.0; // 4 HP = 2 Hearts
        plugin.getConfig().set("player-health-bonuses." + player.getUniqueId(), newBonus);
        plugin.saveConfig();

        applyHealthBonus(player);
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        player.sendMessage(Component.text("You consumed a Dragon Heart and gained 2 permanent hearts!",
                NamedTextColor.LIGHT_PURPLE));
    }
}
