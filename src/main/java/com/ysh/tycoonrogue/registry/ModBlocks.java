package com.ysh.tycoonrogue.registry;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.block.TycoonCounterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TycoonRogue.MODID);

    public static final DeferredBlock<TycoonCounterBlock> TYCOON_COUNTER = BLOCKS.register(
            "tycoon_counter",
            () -> new TycoonCounterBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<com.ysh.tycoonrogue.block.SupplyDeskBlock> SUPPLY_DESK = BLOCKS.register(
            "supply_desk",
            () -> new com.ysh.tycoonrogue.block.SupplyDeskBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<com.ysh.tycoonrogue.block.DisplayStandBlock> DISPLAY_STAND = BLOCKS.register(
            "display_stand",
            () -> new com.ysh.tycoonrogue.block.DisplayStandBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F, 6.0F)
                    .noOcclusion()
                    .lightLevel(state -> 6)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<com.ysh.tycoonrogue.block.AutoRegisterBlock> AUTO_REGISTER = BLOCKS.register(
            "auto_register",
            () -> new com.ysh.tycoonrogue.block.AutoRegisterBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.METAL)));

    private ModBlocks() {
    }
}
