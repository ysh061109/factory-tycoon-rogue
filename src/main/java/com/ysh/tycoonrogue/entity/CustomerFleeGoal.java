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
        if (!customer.isFleeing()) {
            return false;
        }
        this.threat = customer.level().getNearestPlayer(customer, 32.0D);
        return this.threat != null;
    }

    @Override
    public boolean canContinueToUse() {
        return customer.isFleeing() && !customer.getNavigation().isDone();
    }

    @Override
    public void start() {
        Vec3 target = DefaultRandomPos.getPosAway(customer, 24, 8, threat.position());
        if (target != null) {
            customer.getNavigation().moveTo(target.x, target.y, target.z, 1.5D);
        }
    }
}
