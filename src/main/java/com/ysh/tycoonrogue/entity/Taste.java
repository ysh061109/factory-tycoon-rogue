package com.ysh.tycoonrogue.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

/**
 * What a customer is passionate about. Customers rate a factory higher when its showcase
 * chests hold items that match their taste. Item ids from other mods (for example
 * "create:cogwheel") are optional and only count when that mod is installed.
 */
public enum Taste {
    HISTORY("history",
            "minecraft:book", "minecraft:paper", "minecraft:map", "minecraft:clock", "minecraft:bookshelf",
            "minecraft:chiseled_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:enchanted_book"),
    CARS("cars",
            "minecraft:minecart", "minecraft:rail", "minecraft:piston", "minecraft:redstone", "minecraft:iron_ingot",
            "create:cogwheel", "create:large_cogwheel", "create:shaft", "create:brass_ingot"),
    AIRPLANES("airplanes",
            "minecraft:feather", "minecraft:firework_rocket", "minecraft:phantom_membrane", "minecraft:elytra",
            "create:propeller", "create:andesite_alloy", "create:brass_sheet", "create:sail_frame", "create:white_sail",
            "create:windmill_bearing"),
    COFFEE("coffee",
            "minecraft:cocoa_beans", "minecraft:sugar", "minecraft:milk_bucket", "minecraft:cookie",
            "minecraft:glass_bottle"),
    COLA("cola",
            "minecraft:sugar", "minecraft:honey_bottle", "minecraft:sweet_berries", "minecraft:glass_bottle",
            "minecraft:potion");

    private final String id;
    private final String[] itemIds;
    private Set<Item> items;

    Taste(String id, String... itemIds) {
        this.id = id;
        this.itemIds = itemIds;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "taste.tycoonrogue." + id;
    }

    /** The items that fit this taste (only those that exist in this game). */
    public Set<Item> items() {
        if (items == null) {
            Set<Item> found = new HashSet<>();
            for (String name : itemIds) {
                ResourceLocation key = ResourceLocation.tryParse(name);
                if (key != null) {
                    BuiltInRegistries.ITEM.getOptional(key).ifPresent(item -> {
                        if (item != Items.AIR) {
                            found.add(item);
                        }
                    });
                }
            }
            switch (this) {
                case AIRPLANES -> found.addAll(ModCompat.aeronauticsItems());
                case CARS -> found.addAll(ModCompat.offroadItems());
                case COFFEE, COLA -> found.addAll(ModCompat.drinkItems());
                default -> {
                }
            }
            items = found;
        }
        return items;
    }

    public static Taste byId(int index) {
        Taste[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
