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
                    .sound(SoundType.WOOD)));

    private ModBlocks() {
    }
}
