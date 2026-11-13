package com.ysh.tycoonrogue.entity;

import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * New customers walk to the end of the line in front of the Tycoon Counter and wait their turn.
 * When the front customer has stood there for a moment, he is let go to talk to the player.
 */
public class QueueGoal extends Goal {
    private static final int SERVE_AFTER_TICKS = 70;
    private static final int GIVE_UP_TICKS = 2400;

    private final CustomerEntity customer;
    private int waited;
    private int atFront;
    private int recheck;
    private BlockPos slot;

    public QueueGoal(CustomerEntity customer) {
        this.customer = customer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return customer.isQueued() && !customer.isFleeing() && !customer.isScared()
                && customer.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        waited = 0;
        atFront = 0;
        recheck = 0;
        slot = null;
    }

    @Override
    public void tick() {
        if (!(customer.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        BlockPos counter = customer.getQueueCounter();
        if (++waited > GIVE_UP_TICKS || counter == null
                || TycoonData.get(level.getServer()).nearestCounter(level.dimension().location(), counter, 1.0D) == null) {
            customer.leaveQueue();
            return;
        }
        if (recheck-- <= 0) {
            recheck = 10;
            List<CustomerEntity> line = CounterQueue.waiting(level, counter);
            int rank = Math.max(0, line.indexOf(customer));
            slot = CounterQueue.slot(level, counter, rank);
            atFront = rank == 0 ? atFront : 0;
            if (rank == 0) {
                atFront += 10;
            }
        }
        if (slot == null) {
            return;
        }
        double dist = customer.distanceToSqr(slot.getX() + 0.5D, slot.getY(), slot.getZ() + 0.5D);
        if (dist > 1.2D) {
            if (customer.getNavigation().isDone() || recheck == 9) {
                customer.getNavigation().moveTo(slot.getX() + 0.5D, slot.getY(), slot.getZ() + 0.5D, 0.85D);
            }
            return;
        }
        customer.getNavigation().stop();
        customer.getLookControl().setLookAt(counter.getX() + 0.5D, counter.getY() + 1.2D, counter.getZ() + 0.5D);
        if (atFront >= SERVE_AFTER_TICKS) {
            customer.leaveQueue();
        }
    }
}
