package com.ysh.tycoonrogue;

import com.mojang.logging.LogUtils;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import com.ysh.tycoonrogue.registry.ModBlocks;
import com.ysh.tycoonrogue.registry.ModEntities;
import com.ysh.tycoonrogue.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(TycoonRogue.MODID)
public class TycoonRogue {
    public static final String MODID = "tycoonrogue";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TycoonRogue(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        com.ysh.tycoonrogue.registry.ModBlockEntities.TYPES.register(modEventBus);
        modEventBus.addListener(com.ysh.tycoonrogue.registry.ModBlockEntities::registerCapabilities);

        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::addCreativeTabItems);

        LOGGER.info("Factory Tycoon Rogue loaded");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CUSTOMER.get(), CustomerEntity.createAttributes().build());
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.CUSTOMER_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.PHONE);
            event.accept(ModItems.HOLOGRAM_GUIDE);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.TYCOON_COUNTER);
            event.accept(ModItems.SUPPLY_DESK);
            event.accept(ModItems.DISPLAY_STAND);
            event.accept(ModItems.AUTO_REGISTER);
        }
    }
}
