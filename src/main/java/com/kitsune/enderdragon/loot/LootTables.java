package com.kitsune.enderdragon.loot;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTables {

    private static final Random RANDOM = new Random();

    public static List<ItemStack> getLootForLevel(int level) {
        List<ItemStack> loot = new ArrayList<>();

        // Guaranteed Drops
        loot.add(new ItemStack(Material.ELYTRA));

        // Totem Scaling
        int extraTotems = level / 20; // 1 extra every 20 levels
        double extraChance = (level % 20) * 0.05; // 5% chance per level toward the next guaranteed one
        int totemsToGive = 1 + extraTotems + (RANDOM.nextDouble() < extraChance ? 1 : 0);
        for (int i = 0; i < totemsToGive; i++) {
            loot.add(new ItemStack(Material.TOTEM_OF_UNDYING));
        }

        // Spawner Scaling
        int spawners = (level <= 20) ? 1 : (level <= 30) ? 2 : 3;
        for (int i = 0; i < spawners; i++) {
            loot.add(new ItemStack(Material.SPAWNER));
        }

        // Defined Loot (1-20)
        if (level <= 20) {
            addDefinedLoot(loot, level);
        } else {
            addRandomLoot(loot, level);
        }

        return loot;
    }

    private static void addDefinedLoot(List<ItemStack> loot, int level) {
        // Linear scaling helper for quantities
        int diamonds = 16 + (level - 1) * (256 - 16) / 19; // Scales from 16 to 256 (4 stacks)
        int emeralds = 16 + (level - 1) * (256 - 16) / 19;
        int maxBooks = 1 + (level - 1) * 15 / 19; // 1 to 16

        if (diamonds > 0)
            loot.add(new ItemStack(Material.DIAMOND, Math.min(diamonds, 64)));
        if (diamonds > 64)
            loot.add(new ItemStack(Material.DIAMOND, Math.min(diamonds - 64, 64)));
        if (diamonds > 128)
            loot.add(new ItemStack(Material.DIAMOND, Math.min(diamonds - 128, 64)));
        if (diamonds > 192)
            loot.add(new ItemStack(Material.DIAMOND, Math.min(diamonds - 192, 64)));

        if (emeralds > 0)
            loot.add(new ItemStack(Material.EMERALD, Math.min(emeralds, 64)));
        if (emeralds > 64)
            loot.add(new ItemStack(Material.EMERALD, Math.min(emeralds - 64, 64)));
        if (emeralds > 128)
            loot.add(new ItemStack(Material.EMERALD, Math.min(emeralds - 128, 64)));
        if (emeralds > 192)
            loot.add(new ItemStack(Material.EMERALD, Math.min(emeralds - 192, 64)));

        for (int i = 0; i < maxBooks; i++) {
            loot.add(createMaxBook());
        }

        // Tiered Materials
        if (level < 5) {
            loot.add(new ItemStack(Material.NETHERITE_SCRAP, 1 + level / 2));
        } else if (level < 15) {
            loot.add(new ItemStack(Material.NETHERITE_INGOT, 1 + (level - 5) / 3));
            loot.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1 + (level - 5) / 2));
        } else {
            loot.add(new ItemStack(Material.NETHERITE_BLOCK, 1 + (level - 15)));
            loot.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2 + (level - 15) * 2));
        }

        // Rare Blocks
        if (level >= 5)
            loot.add(new ItemStack(Material.GILDED_BLACKSTONE, 4 + (level - 5)));
        if (level >= 10)
            loot.add(new ItemStack(Material.BUDDING_AMETHYST, 8 + (level - 10)));
        if (level >= 15)
            loot.add(new ItemStack(Material.SPONGE, 16 + (level - 15) * 2));
        if (level >= 20)
            loot.add(new ItemStack(Material.BEACON, 16));
    }

    private static void addRandomLoot(List<ItemStack> loot, int level) {
        int rolls = 5 + (level - 20) / 10;
        Material[] pool = {
                Material.NETHERITE_BLOCK,
                Material.ENCHANTED_GOLDEN_APPLE,
                Material.BEACON,
                Material.SPONGE,
                Material.BUDDING_AMETHYST,
                Material.NETHER_STAR,
                Material.TOTEM_OF_UNDYING
        };

        for (int i = 0; i < rolls; i++) {
            Material mat = pool[RANDOM.nextInt(pool.length)];
            int amount = 1 + RANDOM.nextInt(8) + (level / 20);
            loot.add(new ItemStack(mat, Math.min(amount, 64)));
        }

        for (int i = 0; i < 10; i++) {
            loot.add(createMaxBook());
        }
    }

    public static ItemStack createMaxBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        Enchantment[] enchants = {
                Enchantment.SHARPNESS, Enchantment.MENDING, Enchantment.EFFICIENCY,
                Enchantment.PROTECTION, Enchantment.UNBREAKING, Enchantment.FORTUNE
        };

        Enchantment chosen = enchants[RANDOM.nextInt(enchants.length)];
        meta.addStoredEnchant(chosen, chosen.getMaxLevel(), true);
        book.setItemMeta(meta);
        return book;
    }

    public static long getExpForLevel(int level, double xpFactor) {
        // Base dragon XP is roughly 12000 for the first kill, then 500
        long base = (level == 1) ? 12000 : 2000;
        return (long) (base * (1 + xpFactor * level));
    }
}
