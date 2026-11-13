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

    private ModBlockEntities() {
    }
}
