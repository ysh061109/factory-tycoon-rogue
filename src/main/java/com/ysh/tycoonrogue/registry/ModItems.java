package com.ysh.tycoonrogue.registry;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TycoonRogue.MODID);

    public static final DeferredItem<Item> CUSTOMER_SPAWN_EGG = ITEMS.register(
            "customer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CUSTOMER, 0x5E7C16, 0xE8C170, new Item.Properties()));

    private ModItems() {
    }
}
