package com.ysh.tycoonrogue.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/** A robber walks up to the nearest player, grabs some emeralds and runs. */
public class RobPlayerGoal extends Goal {
    private static final int MAX_STOLEN = 24;

    private final CustomerEntity robber;
    private Player target;

    public RobPlayerGoal(CustomerEntity robber) {
        this.robber = robber;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (robber.getJob().role() != CustomerJob.Role.ROBBER || robber.isFleeing() || !robber.getLoot().isEmpty()) {
            return false;
        }
        Player nearest = robber.level().getNearestPlayer(robber, 32.0D);
        if (nearest == null || nearest.isSpectator()) {
            return false;
        }
        this.target = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && !target.isSpectator()
                && !robber.isFleeing() && robber.distanceToSqr(target) < 40.0D * 40.0D;
    }

    @Override
    public void tick() {
        robber.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (robber.distanceToSqr(target) > 2.25D) {
            robber.getNavigation().moveTo(target, 1.15D);
            return;
        }
        steal();
    }

    private void steal() {
        int stolen = 0;
        var inventory = target.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && stolen < MAX_STOLEN; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                int take = Math.min(stack.getCount(), MAX_STOLEN - stolen);
                stack.shrink(take);
                stolen += take;
            }
        }
        if (stolen > 0) {
            robber.getLoot().add(new ItemStack(Items.EMERALD, stolen));
            target.displayClientMessage(Component.translatable("message.tycoonrogue.robber.stole", stolen), false);
        } else {
            target.displayClientMessage(Component.translatable("message.tycoonrogue.robber.empty"), false);
        }
        robber.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.7F);
        robber.startFleeing();
    }
}
