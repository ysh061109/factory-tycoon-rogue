package com.ysh.tycoonrogue.entity;

import com.ysh.tycoonrogue.block.TycoonCounterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/** Works out where each waiting customer stands in the line in front of a Tycoon Counter. */
public final class CounterQueue {
    /** The first customer stands this many blocks in front of the counter. */
    private static final int FIRST_SLOT = 2;
    public static final int MAX_LINE = 6;

    private CounterQueue() {
    }

    public static List<CustomerEntity> waiting(ServerLevel level, BlockPos counter) {
        return level.getEntitiesOfClass(CustomerEntity.class, new AABB(counter).inflate(40.0D),
                        e -> e.isQueued() && counter.equals(e.getQueueCounter())).stream()
                .sorted(Comparator.comparingLong(CustomerEntity::getQueueSince)
                        .thenComparing(e -> e.getUUID()))
                .toList();
    }

    /** The direction the line runs: out of the front of the counter, or any open side if that is blocked. */
    public static Direction lineDirection(ServerLevel level, BlockPos counter) {
        BlockState state = level.getBlockState(counter);
        Direction front = state.getBlock() instanceof TycoonCounterBlock
                ? state.getValue(TycoonCounterBlock.FACING) : Direction.NORTH;
        if (ground(level, counter.relative(front, FIRST_SLOT)) != null) {
            return front;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (ground(level, counter.relative(d, FIRST_SLOT)) != null) {
                return d;
            }
        }
        return front;
    }

    /** Standing position for the customer at {@code rank} (0 = front of the line). */
    public static BlockPos slot(ServerLevel level, BlockPos counter, int rank) {
        Direction dir = lineDirection(level, counter);
        for (int back = rank; back >= 0; back--) {
            BlockPos ground = ground(level, counter.relative(dir, FIRST_SLOT + back));
            if (ground != null) {
                return ground;
            }
        }
        return counter.relative(dir, FIRST_SLOT);
    }

    private static BlockPos ground(ServerLevel level, BlockPos column) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos feet = column.offset(0, dy, 0);
            if (level.getBlockState(feet.below()).isSolid()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return feet;
            }
        }
        return null;
    }
}
