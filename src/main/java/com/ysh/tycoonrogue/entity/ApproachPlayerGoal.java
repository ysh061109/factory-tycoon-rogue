package com.ysh.tycoonrogue.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Customers with an unfinished order walk up to the nearest player to talk.
 * Every disguised thief does this too, so walking up proves nothing.
 */
public class ApproachPlayerGoal extends Goal {
    private static final double STOP_DISTANCE_SQR = 3.0D * 3.0D;

    private final CustomerEntity customer;
    private Player player;

    public ApproachPlayerGoal(CustomerEntity customer) {
        this.customer = customer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (customer.getRole() == CustomerRole.RAIDER || customer.isFleeing()) {
            return false;
        }
        CustomerOrder order = customer.getOrder();
        if (order == null || order.isComplete()) {
            return false;
        }
        Player nearest = customer.level().getNearestPlayer(customer, 24.0D);
        if (nearest == null || nearest.isSpectator() || customer.distanceToSqr(nearest) <= STOP_DISTANCE_SQR) {
            return false;
        }
        this.player = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return player != null && player.isAlive() && !player.isSpectator() && !customer.isFleeing()
                && customer.distanceToSqr(player) > STOP_DISTANCE_SQR;
    }

    @Override
    public void tick() {
        customer.getNavigation().moveTo(player, 0.9D);
    }

    @Override
    public void stop() {
        customer.getNavigation().stop();
    }
}
