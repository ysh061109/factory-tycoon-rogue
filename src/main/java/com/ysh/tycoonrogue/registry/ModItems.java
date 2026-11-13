package com.ysh.tycoonrogue.registry;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.item.PhoneItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TycoonRogue.MODID);

    public static final DeferredItem<Item> CUSTOMER_SPAWN_EGG = ITEMS.register(
            "customer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CUSTOMER, 0x5E7C16, 0xE8C170, new Item.Properties()));

    public static final DeferredItem<PhoneItem> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> TYCOON_COUNTER =
            ITEMS.registerSimpleBlockItem("tycoon_counter", ModBlocks.TYCOON_COUNTER);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> SUPPLY_DESK =
            ITEMS.registerSimpleBlockItem("supply_desk", ModBlocks.SUPPLY_DESK);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> DISPLAY_STAND =
            ITEMS.registerSimpleBlockItem("display_stand", ModBlocks.DISPLAY_STAND);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> AUTO_REGISTER =
            ITEMS.registerSimpleBlockItem("auto_register", ModBlocks.AUTO_REGISTER);

    public static final DeferredItem<com.ysh.tycoonrogue.item.HologramGuideItem> HOLOGRAM_GUIDE = ITEMS.register(
            "hologram_guide", () -> new com.ysh.tycoonrogue.item.HologramGuideItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
