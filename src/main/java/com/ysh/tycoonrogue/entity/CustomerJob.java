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
 */
public enum CustomerJob {
    FARMER("farmer", Role.NORMAL, null, 0.30,
            "minecraft:wheat", "minecraft:bread", "minecraft:carrot", "minecraft:potato", "minecraft:hay_block"),
    BLACKSMITH("blacksmith", Role.NORMAL, null, 0.90,
            "minecraft:iron_ingot", "minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_sword",
            "minecraft:iron_shovel", "create:iron_sheet"),
    CHEF("chef", Role.NORMAL, null, 0.60,
            "minecraft:cooked_beef", "minecraft:bread", "minecraft:cake", "minecraft:cooked_porkchop",
            "minecraft:pumpkin_pie"),
    BARISTA("barista", Role.NORMAL, Taste.COFFEE, 0.50,
            "minecraft:sugar", "minecraft:cocoa_beans", "minecraft:milk_bucket", "minecraft:cookie",
            "minecraft:glass_bottle"),
    HISTORIAN("historian", Role.NORMAL, Taste.HISTORY, 0.70,
            "minecraft:book", "minecraft:paper", "minecraft:map", "minecraft:clock", "minecraft:bookshelf"),
    MECHANIC("mechanic", Role.NORMAL, Taste.CARS, 1.00,
            "minecraft:iron_ingot", "minecraft:redstone", "minecraft:piston", "minecraft:minecart",
            "create:cogwheel", "create:shaft", "create:brass_ingot"),
    PILOT("pilot", Role.NORMAL, Taste.AIRPLANES, 1.10,
            "minecraft:feather", "minecraft:firework_rocket", "minecraft:phantom_membrane", "minecraft:compass",
            "create:andesite_alloy", "create:brass_sheet"),
    MINER("miner", Role.NORMAL, null, 0.80,
            "minecraft:coal", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:torch",
            "minecraft:iron_pickaxe", "minecraft:diamond"),
    BUILDER("builder", Role.NORMAL, null, 0.40,
            "minecraft:bricks", "minecraft:stone_bricks", "minecraft:glass", "minecraft:oak_planks",
            "minecraft:smooth_stone", "create:andesite_alloy"),
    SODA_VENDOR("soda_vendor", Role.NORMAL, Taste.COLA, 0.55,
            "minecraft:sugar", "minecraft:glass_bottle", "minecraft:honey_bottle", "minecraft:sweet_berries",
            "minecraft:cocoa_beans"),
    ROBBER("robber", Role.ROBBER, null, 0.0),
    SCAMMER("scammer", Role.SCAMMER, null, 0.80,
            "minecraft:diamond", "minecraft:gold_ingot", "minecraft:iron_ingot", "minecraft:emerald",
            "minecraft:iron_pickaxe", "minecraft:cake");

    public enum Role {NORMAL, ROBBER, SCAMMER}

    private final String id;
    private final Role role;
    private final Taste fixedTaste;
    private final double unitPrice;
    private final String[] wantedItems;

    CustomerJob(String id, Role role, Taste fixedTaste, double unitPrice, String... wantedItems) {
        this.id = id;
        this.role = role;
        this.fixedTaste = fixedTaste;
        this.unitPrice = unitPrice;
        this.wantedItems = wantedItems;
    }

    public String id() {
        return id;
    }

    public Role role() {
        return role;
    }

    /** Emeralds paid per delivered item (before the customer's generosity bonus). */
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

    /** Items from {@link #wantedItems} that actually exist in this game (other mods may be missing). */
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
        return items;
    }

    public static CustomerJob byIndex(int index) {
        CustomerJob[] values = values();
        return values[Math.floorMod(index, values.length)];
    }

    /** A random job for an ordinary customer (never a robber or scammer). */
    public static CustomerJob randomNormal(RandomSource random) {
        List<CustomerJob> normal = new ArrayList<>();
        for (CustomerJob job : values()) {
            if (job.role == Role.NORMAL) {
                normal.add(job);
            }
        }
        return normal.get(random.nextInt(normal.size()));
    }
}
