package com.ysh.tycoonrogue.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The armed raid. A raider charges the nearest player, takes half of the player's emeralds
 * (16 to 256), then runs to up to two nearby chests and ransacks them, and finally flees.
 * Kill the raider before he escapes to get everything back.
 */
public class RaiderGoal extends Goal {
    private enum Phase {CHARGE, RANSACK, DONE}

    private static final int MIN_STOLEN = 16;
    private static final int MAX_STOLEN = 256;
    private static final int STACKS_PER_CONTAINER = 3;
    private static final int MAX_CONTAINERS = 2;
    private static final int TIMEOUT_TICKS = 900;

    private final CustomerEntity raider;
    private final List<BlockPos> containers = new ArrayList<>();
    private Player target;
    private Phase phase = Phase.DONE;
    private int ticks;

    public RaiderGoal(CustomerEntity raider) {
        this.raider = raider;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (raider.getRole() != CustomerRole.RAIDER || raider.isFleeing() || !raider.getLoot().isEmpty()) {
            return false;
        }
        Player nearest = raider.level().getNearestPlayer(raider, 64.0D);
        if (nearest == null || nearest.isSpectator()) {
            return false;
        }
        this.target = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return phase != Phase.DONE && !raider.isFleeing() && target != null && target.isAlive();
    }

    @Override
    public void start() {
        phase = Phase.CHARGE;
        ticks = 0;
        containers.clear();
        raider.setState(CustomerEntity.STATE_CHARGE);
        raider.playSound(SoundEvents.PILLAGER_CELEBRATE, 2.0F, 0.8F);
        raider.say(Dialogue.Situation.RAIDER_CHARGE);
    }

    @Override
    public void tick() {
        if (++ticks > TIMEOUT_TICKS) {
            finish();
            return;
        }
        if (phase == Phase.CHARGE) {
            raider.getLookControl().setLookAt(target, 30.0F, 30.0F);
            raider.getNavigation().moveTo(target, 1.3D);
            if (raider.distanceToSqr(target) <= 2.5D * 2.5D) {
                robPlayer();
            }
        } else if (phase == Phase.RANSACK) {
            ransackNext();
        }
    }

    private void robPlayer() {
        int have = TheftHelper.countEmeralds(target);
        int want = Math.max(Math.min(MIN_STOLEN, have), Math.min(MAX_STOLEN, have / 2));
        int stolen = TheftHelper.stealEmeralds(target, want);
        if (stolen > 0) {
            raider.getLoot().add(new ItemStack(Items.EMERALD, stolen));
            target.displayClientMessage(Component.translatable("message.tycoonrogue.raider.stole", stolen), false);
        }
        raider.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.5F, 0.7F);
        if (raider.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2D, target.getZ(),
                    30, 0.5D, 0.6D, 0.5D, 0.3D);
            findContainers(level);
        }
        if (containers.isEmpty()) {
            finish();
        } else {
            phase = Phase.RANSACK;
            raider.setState(CustomerEntity.STATE_RANSACK);
            target.displayClientMessage(Component.translatable("message.tycoonrogue.raider.ransack"), false);
        }
    }

    /** Looks for chests, barrels and the like around the player. */
    private void findContainers(ServerLevel level) {
        BlockPos center = target.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -4, -10), center.offset(10, 4, 10))) {
            if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container && !container.isEmpty()) {
                found.add(pos.immutable());
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(raider.blockPosition())));
        for (int i = 0; i < Math.min(MAX_CONTAINERS, found.size()); i++) {
            containers.add(found.get(i));
        }
    }

    private void ransackNext() {
        if (containers.isEmpty()) {
            finish();
            return;
        }
        BlockPos pos = containers.get(0);
        if (raider.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 2.5D * 2.5D) {
            if (ticks % 10 == 0) {
                raider.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.3D);
            }
            return;
        }
        containers.remove(0);
        if (raider.level() instanceof ServerLevel level
                && level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container) {
            List<Integer> slots = new ArrayList<>();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) {
                    slots.add(slot);
                }
            }
            for (int n = 0; n < STACKS_PER_CONTAINER && !slots.isEmpty(); n++) {
                int slot = slots.remove(level.random.nextInt(slots.size()));
                raider.getLoot().add(container.removeItemNoUpdate(slot));
            }
            container.setChanged();
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    14, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        if (containers.isEmpty()) {
            finish();
        }
    }

    private void finish() {
        phase = Phase.DONE;
        raider.say(Dialogue.Situation.RAIDER_FLEE);
        raider.startFleeing();
    }

    @Override
    public void stop() {
        phase = Phase.DONE;
    }
}
