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
            "aeronautics", "simulated", "create_aeronautics", "createaeronautics", "create_simulated",
            // Create Propulsion (thrusters for Simulated)
            "createpropulsion", "create_propulsion", "propulsion", "create_propulsion_simulated",
            "createpropulsionsimulated");
    /** Create Offroad (wheels and land vehicles from the same project). */
    public static final Set<String> OFFROAD = Set.of("offroad", "create_offroad", "createoffroad");
    /** Create: Ratatouille (cooking machines and food). */
    public static final Set<String> RATATOUILLE = Set.of(
            "create_ratatouille", "ratatouille", "createratatouille");
    /** Farmer's Delight (cooking pot, stove, skillet, cutting board and lots of food). */
    public static final String FARMERS_DELIGHT = "farmersdelight";
    private static final Set<String> FD_MACHINES = Set.of("cooking_pot", "stove", "skillet", "cutting_board",
            "basket", "stove_top", "rice_crate");

    /** Create: Ratatouille and its add-ons: the exact id is not certain, so match on the name. */
    public static boolean isRatatouille(String namespace) {
        return RATATOUILLE.contains(namespace) || namespace.contains("ratatouille");
    }

    /** True for a Farmer's Delight or Ratatouille block that works like a kitchen machine. */
    public static boolean isKitchenMachine(String namespace, String path) {
        if (namespace.equals(FARMERS_DELIGHT)) {
            return FD_MACHINES.contains(path);
        }
        return isRatatouille(namespace);
    }

    private static boolean isFoodNamespace(String namespace) {
        return namespace.equals(FARMERS_DELIGHT) || isRatatouille(namespace);
    }

    private static final String[] DRINK_WORDS = {
            "coffee", "tea", "cider", "juice", "cola", "soda", "drink", "milk", "cocoa", "smoothie", "lemonade", "shake"};

    private static List<Item> aeronautics;
    private static List<Item> offroad;
    private static List<Item> food;
    private static List<Item> drinks;

    private ModCompat() {
    }

    /** True for blocks of mods whose machines count towards a factory's size. */
    public static boolean isMachineNamespace(String namespace) {
        return namespace.equals("create") || AERONAUTICS.contains(namespace) || OFFROAD.contains(namespace)
                || isRatatouille(namespace);
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

    /** Wheels and vehicle parts, for mechanics and car lovers. */
    public static List<Item> offroadItems() {
        if (offroad == null) {
            List<Item> found = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (entry.getValue() != Items.AIR && OFFROAD.contains(entry.getKey().location().getNamespace())) {
                    found.add(entry.getValue());
                }
            }
            offroad = found;
        }
        return offroad;
    }

    /** Edible items from Ratatouille and Farmer's Delight. */
    public static List<Item> foodItems() {
        if (food == null) {
            List<Item> found = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                Item item = entry.getValue();
                if (isFoodNamespace(entry.getKey().location().getNamespace())
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
