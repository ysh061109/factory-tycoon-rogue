package com.ysh.tycoonrogue.shop;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * One thing the Supply Desk sells. Texts come from the keys shop.tycoonrogue.&lt;id&gt;.name / .desc.
 * Prices are in emeralds (paid in Numismatics coins when that mod is installed).
 */
public record ShopOffer(String id, int price, Supplier<ItemStack> icon, Supplier<List<ItemStack>> goods,
                        boolean needsAeronautics) {
    public String nameKey() {
        return "shop.tycoonrogue." + id + ".name";
    }

    public String descKey() {
        return "shop.tycoonrogue." + id + ".desc";
    }
}
