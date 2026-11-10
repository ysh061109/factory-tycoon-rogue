package com.ysh.tycoonrogue.entity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared helpers for thieves taking emeralds out of a player's inventory. */
public final class TheftHelper {
    private TheftHelper() {
    }

    public static int countEmeralds(Player player) {
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Removes up to {@code max} emeralds from the player and returns how many were taken. */
    public static int stealEmeralds(Player player, int max) {
        int stolen = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && stolen < max; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                int take = Math.min(stack.getCount(), max - stolen);
                stack.shrink(take);
                stolen += take;
            }
        }
        return stolen;
    }
}
