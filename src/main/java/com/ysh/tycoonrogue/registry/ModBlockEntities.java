package com.ysh.tycoonrogue.registry;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.block.DisplayStandBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TycoonRogue.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayStandBlockEntity>> DISPLAY_STAND =
            TYPES.register("display_stand", () -> BlockEntityType.Builder.of(
                    DisplayStandBlockEntity::new, ModBlocks.DISPLAY_STAND.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.ysh.tycoonrogue.block.AutoRegisterBlockEntity>> AUTO_REGISTER =
            TYPES.register("auto_register", () -> BlockEntityType.Builder.of(
                    com.ysh.tycoonrogue.block.AutoRegisterBlockEntity::new, ModBlocks.AUTO_REGISTER.get()).build(null));

    /** Lets hoppers, Create and other mods pull the money out of the register. */
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                AUTO_REGISTER.get(), (be, side) -> be.output());
    }

    private ModBlockEntities() {
    }
}
