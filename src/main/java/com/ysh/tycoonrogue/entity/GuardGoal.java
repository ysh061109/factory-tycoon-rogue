package com.ysh.tycoonrogue.entity;

import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * What a hired guard does all day:
 * - spots disguised robbers and scammers standing close by and exposes them (they run),
 * - chases down raiders and exposed or fleeing thieves and hits them with a sword,
 * - otherwise stays near the player and the nearest Tycoon Counter.
 */
public class GuardGoal extends Goal {
    private static final double SEARCH = 22.0D;
    private static final double SPOT_RANGE = 9.0D;

    private final CustomerEntity guard;
    private CustomerEntity target;
    private int attackCooldown;
    private int patrolCooldown;

    public GuardGoal(CustomerEntity guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return guard.getRole() == CustomerRole.GUARD;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private static boolean isCriminal(CustomerEntity e) {
        if (!e.isAlive()) {
            return false;
        }
        return switch (e.getRole()) {
            case RAIDER -> true;
            case ROBBER, SCAMMER -> e.getReveal() != 0 || e.isFleeing();
            default -> false;
        };
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (guard.tickCount % 10 == 0) {
            if (target == null || !isCriminal(target) || target.distanceToSqr(guard) > SEARCH * SEARCH * 1.6D) {
                target = findCriminal();
            }
            if (target == null && guard.tickCount % 40 == 0) {
                spotDisguised();
            }
        }
        if (target != null) {
            chase();
        } else {
            patrol();
        }
    }

    private CustomerEntity findCriminal() {
        List<CustomerEntity> near = guard.level().getEntitiesOfClass(CustomerEntity.class,
                guard.getBoundingBox().inflate(SEARCH), GuardGoal::isCriminal);
        CustomerEntity best = null;
        for (CustomerEntity e : near) {
            if (best == null || e.distanceToSqr(guard) < best.distanceToSqr(guard)) {
                best = e;
            }
        }
        if (best != null && best != target) {
            guard.say(Dialogue.Situation.GUARD_SPOT);
        }
        return best;
    }

    /** A guard has a trained eye: each check has a chance to expose a disguised thief close by. */
    private void spotDisguised() {
        List<CustomerEntity> near = guard.level().getEntitiesOfClass(CustomerEntity.class,
                guard.getBoundingBox().inflate(SPOT_RANGE),
                e -> e.isAlive() && (e.getRole() == CustomerRole.ROBBER || e.getRole() == CustomerRole.SCAMMER)
                        && e.getReveal() == 0);
        for (CustomerEntity thief : near) {
            if (guard.getRandom().nextFloat() < 0.35F) {
                thief.reveal();
                thief.startFleeing();
                guard.say(Dialogue.Situation.GUARD_SPOT);
                target = thief;
                return;
            }
        }
    }

    private void chase() {
        guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distSqr = guard.distanceToSqr(target);
        if (distSqr > 2.6D * 2.6D) {
            guard.getNavigation().moveTo(target, 1.35D);
            return;
        }
        guard.getNavigation().stop();
        if (attackCooldown <= 0) {
            attackCooldown = 18;
            guard.swing(InteractionHand.MAIN_HAND);
            float damage = (float) guard.getAttributeValue(Attributes.ATTACK_DAMAGE);
            target.hurt(guard.damageSources().mobAttack(guard), damage);
        }
    }

    private void patrol() {
        if (--patrolCooldown > 0) {
            return;
        }
        patrolCooldown = 60 + guard.getRandom().nextInt(60);
        BlockPos anchor = null;
        Player player = guard.level().getNearestPlayer(guard, 64.0D);
        if (guard.level() instanceof ServerLevel level && level.getServer() != null) {
            anchor = TycoonData.get(level.getServer())
                    .nearestCounter(level.dimension().location(), guard.blockPosition(), 40.0D);
        }
        if (anchor == null && player != null) {
            anchor = player.blockPosition();
        }
        if (anchor == null) {
            return;
        }
        double x = anchor.getX() + guard.getRandom().nextInt(9) - 4;
        double z = anchor.getZ() + guard.getRandom().nextInt(9) - 4;
        guard.getNavigation().moveTo(x, anchor.getY(), z, 0.8D);
    }
}
