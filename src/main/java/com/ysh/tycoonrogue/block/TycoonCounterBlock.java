package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.entity.Taste;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The factory evaluation desk. Visiting customers walk up to it, look at the factory around it
 * and rate it. Right-click it to see the factory report yourself.
 */
public class TycoonCounterBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public TycoonCounterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING,
                net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            TycoonData.get(serverLevel.getServer()).addCounter(level.dimension().location(), pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
            TycoonData.get(serverLevel.getServer()).removeCounter(level.dimension().location(), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            // Make sure counters placed before this feature existed are known to customers.
            TycoonData data = TycoonData.get(serverLevel.getServer());
            data.addCounter(level.dimension().location(), pos);
            FactoryEvaluator.Report report = FactoryEvaluator.evaluate(serverLevel, pos);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.header"), false);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.machines",
                    report.machineBlocks(), report.machineTypes(), report.scale(), report.variety()), false);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.output",
                    data.getRevenue(), data.getQuota(), report.output()), false);
            StringBuilder tastes = new StringBuilder();
            for (Taste taste : Taste.values()) {
                if (tastes.length() > 0) {
                    tastes.append(", ");
                }
                tastes.append(Component.translatable(taste.translationKey()).getString())
                        .append(' ').append(FactoryEvaluator.showcaseScore(report, taste)).append("/25");
            }
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.operation",
                    report.active(), report.operation()), false);
            StringBuilder palette = new StringBuilder();
            Integer[] order = new Integer[16];
            for (int i = 0; i < 16; i++) {
                order[i] = i;
            }
            java.util.Arrays.sort(order, (a, b) -> report.colors()[b] - report.colors()[a]);
            for (int i = 0; i < 3; i++) {
                if (report.colors()[order[i]] > 0) {
                    if (palette.length() > 0) {
                        palette.append(", ");
                    }
                    palette.append(Component.translatable("color.minecraft."
                            + net.minecraft.world.item.DyeColor.byId(order[i]).getName()).getString())
                            .append(' ').append(report.colors()[order[i]]);
                }
            }
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.art",
                    report.art(), palette.length() == 0 ? "-" : palette.toString()), false);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.vehicles",
                    report.aircraft(), report.cars()), false);
            int flow = com.ysh.tycoonrogue.event.ProductionTracker.flowScore(serverLevel, pos);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.flow",
                    flow, Math.min(24, (flow * 16 + 9) / 10)), false);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.parts",
                    report.beltCount(), report.wheelCount(), report.thrusterCount(), report.propellerCount(),
                    report.sailCount(), report.engineCount(), report.balloonCount()), false);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.history",
                    report.roofs(), report.dancheong(), report.libraries(), report.historyScore()), false);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.counter.showcase",
                    tastes.toString()), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
