package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The auto register: customers who pick something from a display stand walk here and pay by
 * themselves, so you do not have to stand at the counter to take their money.
 */
public class AutoRegisterBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<AutoRegisterBlock> CODEC = simpleCodec(AutoRegisterBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public AutoRegisterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoRegisterBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AutoRegisterBlockEntity be ? be.signal() : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            TycoonData.get(sl.getServer()).addKind("register", level.dimension().location(), pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel sl && !newState.is(this)) {
            TycoonData.get(sl.getServer()).removeKind("register", level.dimension().location(), pos);
            if (level.getBlockEntity(pos) instanceof AutoRegisterBlockEntity be) {
                for (net.minecraft.world.item.ItemStack stack : be.takeAll()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AutoRegisterBlockEntity be) {
            // Right-click empties the cash box into your inventory.
            var cash = be.takeAll();
            if (cash.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.tycoonrogue.register.info"), true);
            } else {
                for (net.minecraft.world.item.ItemStack stack : cash) {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
                player.displayClientMessage(Component.translatable("message.tycoonrogue.register.collected"), true);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
