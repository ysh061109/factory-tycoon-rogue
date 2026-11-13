package com.ysh.tycoonrogue.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Runs away from the nearest player while the customer is fleeing (after a theft or a scam). */
public class CustomerFleeGoal extends Goal {
    private final CustomerEntity customer;
    private Player threat;

    public CustomerFleeGoal(CustomerEntity customer) {
        this.customer = customer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!customer.isFleeing() && !customer.isScared()) {
            return false;
        }
        this.threat = customer.level().getNearestPlayer(customer, 48.0D);
        return this.threat != null;
    }

    @Override
    public boolean canContinueToUse() {
        return (customer.isFleeing() || customer.isScared()) && !customer.getNavigation().isDone();
    }

    @Override
    public void start() {
        Vec3 target = DefaultRandomPos.getPosAway(customer, 28, 8, threat.position());
        if (target != null) {
            double speed = customer.getRole() == CustomerRole.RAIDER ? 1.05D
                    : customer.getRole() == CustomerRole.NORMAL ? 1.2D : 0.9D;
            if (customer.level() instanceof net.minecraft.server.level.ServerLevel sl
                    && customer.getRole() != CustomerRole.NORMAL) {
                speed *= com.ysh.tycoonrogue.data.RunStats.fleeMultiplier(
                        com.ysh.tycoonrogue.data.TycoonData.get(sl.getServer()));
            }
            customer.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }
}
