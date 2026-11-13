package com.ysh.tycoonrogue.entity;

import com.ysh.tycoonrogue.data.Feed;
import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

/**
 * A disguised robber chats like any customer. After standing next to the player for a few
 * seconds, he drops the disguise, grabs some emeralds and runs.
 */
public class DisguisedRobberGoal extends Goal {
    private static final int MAX_STOLEN = 32;
    private static final double CLOSE_DISTANCE_SQR = 3.5D * 3.5D;

    private final CustomerEntity robber;
    private Player target;
    private int suspicion;
    private int strikeAt;

    public DisguisedRobberGoal(CustomerEntity robber) {
        this.robber = robber;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (robber.getRole() != CustomerRole.ROBBER || robber.isQueued() || robber.isFleeing() || robber.isScared()
                || !robber.getLoot().isEmpty()) {
            return false;
        }
        Player nearest = robber.level().getNearestPlayer(robber, 4.0D);
        if (nearest == null || nearest.isSpectator() || robber.distanceToSqr(nearest) > CLOSE_DISTANCE_SQR) {
            return false;
        }
        this.target = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && !target.isSpectator() && !robber.isFleeing()
                && robber.distanceToSqr(target) <= CLOSE_DISTANCE_SQR * 2.0D;
    }

    @Override
    public void start() {
        suspicion = 0;
        strikeAt = 100 + robber.getRandom().nextInt(140); // 5 to 12 seconds
    }

    @Override
    public void tick() {
        robber.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (++suspicion < strikeAt) {
            return;
        }
        robber.reveal();
        List<ItemStack> stolen = Currency.take(target, MAX_STOLEN);
        int value = Currency.valueOf(stolen);
        if (value > 0) {
            robber.getLoot().addAll(stolen);
            target.displayClientMessage(Component.translatable("message.tycoonrogue.robber.stole", value), false);
            if (target.getServer() != null) {
                TycoonData data = TycoonData.get(target.getServer());
                data.changeReputation(-3, target, "reason.tycoonrogue.robbed");
                Feed.news(target.getServer(), "robbery");
            }
        } else {
            target.displayClientMessage(Component.translatable("message.tycoonrogue.robber.empty"), false);
        }
        robber.startFleeing();
    }
}
