package com.ysh.tycoonrogue.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Optional support for other Create add-ons. Nothing here needs the mods at compile time: items
 * and blocks are found by their mod id in the registry, so it works when they are installed and
 * silently does nothing when they are not.
 *
 * Mod ids are listed with a few spellings because add-ons are not consistent about them.
 */
public final class ModCompat {
    /** Create Aeronautics and the physics library it is built on. */
    public static final Set<String> AERONAUTICS = Set.of(
            "aeronautics", "create_aeronautics", "createaeronautics", "simulated", "create_simulated");
    /** Create: Ratatouille (cooking machines and food). */
    public static final Set<String> RATATOUILLE = Set.of(
            "create_ratatouille", "ratatouille", "createratatouille");

    private static final String[] DRINK_WORDS = {
            "coffee", "tea", "juice", "cola", "soda", "drink", "milk", "cocoa", "smoothie", "lemonade", "shake"};

    private static List<Item> aeronautics;
    private static List<Item> food;
    private static List<Item> drinks;

    private ModCompat() {
    }

    /** True for blocks of mods whose machines count towards a factory's size. */
    public static boolean isMachineNamespace(String namespace) {
        return namespace.equals("create") || AERONAUTICS.contains(namespace) || RATATOUILLE.contains(namespace);
    }

    public static boolean isAeronauticsLoaded() {
        return !aeronauticsItems().isEmpty();
    }

    public static List<Item> aeronauticsItems() {
        if (aeronautics == null) {
            List<Item> found = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                Item item = entry.getValue();
                if (item != Items.AIR && AERONAUTICS.contains(entry.getKey().location().getNamespace())
                        && !entry.getKey().location().getPath().endsWith("_spawn_egg")) {
                    found.add(item);
                }
            }
            aeronautics = found;
        }
        return aeronautics;
    }

    /** Edible items from Ratatouille. */
    public static List<Item> foodItems() {
        if (food == null) {
            List<Item> found = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                Item item = entry.getValue();
                if (RATATOUILLE.contains(entry.getKey().location().getNamespace())
                        && item.components().has(DataComponents.FOOD)) {
                    found.add(item);
                }
            }
            food = found;
        }
        return food;
    }

    /** Drinks and sweet things from Ratatouille, for baristas and soda vendors. */
    public static List<Item> drinkItems() {
        if (drinks == null) {
            List<Item> found = new ArrayList<>();
            for (Item item : foodItems()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                for (String word : DRINK_WORDS) {
                    if (key.getPath().contains(word)) {
                        found.add(item);
                        break;
                    }
                }
            }
            drinks = found;
        }
        return drinks;
    }
}
