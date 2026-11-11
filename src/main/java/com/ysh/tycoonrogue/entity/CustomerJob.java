package com.ysh.tycoonrogue.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * A customer's occupation. It decides the outfit, which goods the customer orders, and the price.
 * Item ids that belong to other mods (for example "create:cogwheel") are optional: they are used
 * only when that mod is installed.
 *
 * Robbers and scammers get one of the ordinary jobs as their disguise. RAIDER is the only job
 * that is never a disguise.
 */
public enum CustomerJob {
    FARMER("farmer", null, 0.30,
            "minecraft:wheat", "minecraft:bread", "minecraft:carrot", "minecraft:potato", "minecraft:hay_block"),
    BLACKSMITH("blacksmith", null, 0.90,
            "minecraft:iron_ingot", "minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_sword",
            "minecraft:iron_shovel", "create:iron_sheet"),
    CHEF("chef", null, 0.60,
            "minecraft:cooked_beef", "minecraft:bread", "minecraft:cake", "minecraft:cooked_porkchop",
            "minecraft:pumpkin_pie"),
    BARISTA("barista", Taste.COFFEE, 0.50,
            "minecraft:sugar", "minecraft:cocoa_beans", "minecraft:milk_bucket", "minecraft:cookie",
            "minecraft:glass_bottle"),
    HISTORIAN("historian", Taste.HISTORY, 0.70,
            "minecraft:book", "minecraft:paper", "minecraft:map", "minecraft:clock", "minecraft:bookshelf"),
    MECHANIC("mechanic", Taste.CARS, 1.00,
            "minecraft:iron_ingot", "minecraft:redstone", "minecraft:piston", "minecraft:minecart",
            "create:cogwheel", "create:shaft", "create:brass_ingot"),
    PILOT("pilot", Taste.AIRPLANES, 1.10,
            "minecraft:feather", "minecraft:firework_rocket", "minecraft:phantom_membrane", "minecraft:compass",
            "create:andesite_alloy", "create:brass_sheet"),
    MINER("miner", null, 0.80,
            "minecraft:coal", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:torch",
            "minecraft:iron_pickaxe", "minecraft:diamond"),
    BUILDER("builder", null, 0.40,
            "minecraft:bricks", "minecraft:stone_bricks", "minecraft:glass", "minecraft:oak_planks",
            "minecraft:smooth_stone", "create:andesite_alloy"),
    SODA_VENDOR("soda_vendor", Taste.COLA, 0.55,
            "minecraft:sugar", "minecraft:glass_bottle", "minecraft:honey_bottle", "minecraft:sweet_berries",
            "minecraft:cocoa_beans"),
    /** Armed raider outfit. Orders nothing. */
    RAIDER("raider", null, 0.0),
    /** Hired guard. Orders nothing. Always the last entry. */
    GUARD("guard", null, 0.0);

    private final String id;
    private final Taste fixedTaste;
    private final double unitPrice;
    private final String[] wantedItems;

    CustomerJob(String id, Taste fixedTaste, double unitPrice, String... wantedItems) {
        this.id = id;
        this.fixedTaste = fixedTaste;
        this.unitPrice = unitPrice;
        this.wantedItems = wantedItems;
    }

    public String id() {
        return id;
    }

    /** Emeralds paid per delivered item (before price variation and the generosity bonus). */
    public double unitPrice() {
        return unitPrice;
    }

    /** The taste this job always has, or null when the customer's taste is random. */
    public Taste fixedTaste() {
        return fixedTaste;
    }

    public String translationKey() {
        return "job.tycoonrogue." + id;
    }

    public ResourceLocation textureLocation() {
        return ResourceLocation.fromNamespaceAndPath("tycoonrogue", "textures/entity/customer/job/" + id + ".png");
    }

    /** Items from the wanted list that actually exist in this game (other mods may be missing). */
    public List<Item> availableItems() {
        List<Item> items = new ArrayList<>();
        for (String name : wantedItems) {
            ResourceLocation key = ResourceLocation.tryParse(name);
            if (key == null) {
                continue;
            }
            BuiltInRegistries.ITEM.getOptional(key).ifPresent(item -> {
                if (item != Items.AIR) {
                    items.add(item);
                }
            });
        }
        // Goods from other Create add-ons, when installed.
        switch (this) {
            case PILOT -> items.addAll(ModCompat.aeronauticsItems());
            case CHEF -> items.addAll(ModCompat.foodItems());
            case BARISTA, SODA_VENDOR -> items.addAll(ModCompat.drinkItems());
            default -> {
            }
        }
        return items;
    }

    public static CustomerJob byIndex(int index) {
        CustomerJob[] values = values();
        return values[Math.floorMod(index, values.length)];
    }

    /** A random ordinary job. Also used as the disguise of robbers and scammers. */
    public static CustomerJob randomCivilian(RandomSource random) {
        CustomerJob[] values = values();
        return values[random.nextInt(values.length - 2)]; // RAIDER and GUARD are the last two entries
    }
}
