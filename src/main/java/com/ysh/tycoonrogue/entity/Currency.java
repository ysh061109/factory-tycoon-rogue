package com.ysh.tycoonrogue.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Money. Emeralds are the base currency. If Create: Numismatics is installed, payments are made
 * in its coins instead (all six coin items must exist, otherwise emeralds are used).
 *
 * Values are in spurs, the smallest coin. One emerald is worth {@link #SPURS_PER_EMERALD} spurs;
 * change that number to rebalance the two currencies.
 */
public final class Currency {
    public static final int SPURS_PER_EMERALD = 8;

    /** Coin item ids with their value in spurs, biggest first. */
    private static final String[] COIN_IDS = {
            "numismatics:sun", "numismatics:crown", "numismatics:cog",
            "numismatics:sprocket", "numismatics:bevel", "numismatics:spur"};
    private static final int[] COIN_VALUES = {4096, 512, 64, 16, 8, 1};

    private Currency() {
    }

    /** The six coin items, biggest first, or null when Numismatics is not installed. */
    private static Item[] coins() {
        Item[] items = new Item[COIN_IDS.length];
        for (int i = 0; i < COIN_IDS.length; i++) {
            ResourceLocation key = ResourceLocation.tryParse(COIN_IDS[i]);
            Optional<Item> item = key == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(key);
            if (item.isEmpty() || item.get() == Items.AIR) {
                return null;
            }
            items[i] = item.get();
        }
        return items;
    }

    public static boolean usesCoins() {
        return coins() != null;
    }

    /** The items that make up a payment worth {@code emeralds}. */
    public static List<ItemStack> stacksFor(int emeralds) {
        List<ItemStack> stacks = new ArrayList<>();
        if (emeralds <= 0) {
            return stacks;
        }
        Item[] coins = coins();
        if (coins == null) {
            int left = emeralds;
            while (left > 0) {
                int batch = Math.min(64, left);
                stacks.add(new ItemStack(Items.EMERALD, batch));
                left -= batch;
            }
            return stacks;
        }
        long spurs = (long) emeralds * SPURS_PER_EMERALD;
        for (int i = 0; i < coins.length; i++) {
            long count = spurs / COIN_VALUES[i];
            spurs -= count * COIN_VALUES[i];
            while (count > 0) {
                int batch = (int) Math.min(64, count);
                stacks.add(new ItemStack(coins[i], batch));
                count -= batch;
            }
        }
        return stacks;
    }

    /** Gives the player a payment worth {@code emeralds}. What does not fit is dropped at their feet. */
    public static void pay(Player player, int emeralds) {
        for (ItemStack stack : stacksFor(emeralds)) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /** How much the player carries, in emeralds (coins count when Numismatics is installed). */
    public static int balance(Player player) {
        long spurs = 0;
        Item[] coins = coins();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                spurs += (long) stack.getCount() * SPURS_PER_EMERALD;
            } else if (coins != null) {
                for (int i = 0; i < coins.length; i++) {
                    if (stack.is(coins[i])) {
                        spurs += (long) stack.getCount() * COIN_VALUES[i];
                    }
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, spurs / SPURS_PER_EMERALD);
    }

    /**
     * Takes up to {@code maxEmeralds} worth of money out of the player's inventory, smallest
     * coins first so nobody has to make change. Returns what was taken.
     */
    public static List<ItemStack> take(Player player, int maxEmeralds) {
        List<ItemStack> taken = new ArrayList<>();
        long budget = (long) maxEmeralds * SPURS_PER_EMERALD;
        var inventory = player.getInventory();
        Item[] coins = coins();

        // Emeralds first, then coins from the smallest up.
        for (int slot = 0; slot < inventory.getContainerSize() && budget >= SPURS_PER_EMERALD; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                int take = (int) Math.min(stack.getCount(), budget / SPURS_PER_EMERALD);
                stack.shrink(take);
                taken.add(new ItemStack(Items.EMERALD, take));
                budget -= (long) take * SPURS_PER_EMERALD;
            }
        }
        if (coins != null) {
            for (int i = coins.length - 1; i >= 0 && budget > 0; i--) {
                for (int slot = 0; slot < inventory.getContainerSize() && budget >= COIN_VALUES[i]; slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (stack.is(coins[i])) {
                        int take = (int) Math.min(stack.getCount(), budget / COIN_VALUES[i]);
                        stack.shrink(take);
                        taken.add(new ItemStack(coins[i], take));
                        budget -= (long) take * COIN_VALUES[i];
                    }
                }
            }
        }
        return taken;
    }

    /** The value of a list of stacks, in emeralds. */
    public static int valueOf(List<ItemStack> stacks) {
        long spurs = 0;
        Item[] coins = coins();
        for (ItemStack stack : stacks) {
            if (stack.is(Items.EMERALD)) {
                spurs += (long) stack.getCount() * SPURS_PER_EMERALD;
            } else if (coins != null) {
                for (int i = 0; i < coins.length; i++) {
                    if (stack.is(coins[i])) {
                        spurs += (long) stack.getCount() * COIN_VALUES[i];
                    }
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, spurs / SPURS_PER_EMERALD);
    }
}
