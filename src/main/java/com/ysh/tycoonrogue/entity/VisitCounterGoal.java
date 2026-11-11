package com.ysh.tycoonrogue.entity;

import com.ysh.tycoonrogue.block.FactoryEvaluator;
import com.ysh.tycoonrogue.data.Feed;
import com.ysh.tycoonrogue.data.RunStats;
import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * After finishing an order, a happy customer walks to the nearest Tycoon Counter, looks around the
 * factory, rates it (1-5 stars), says so, posts a review on SNS and leaves a tip on the counter.
 */
public class VisitCounterGoal extends Goal {
    private static final double SEARCH_RANGE = 48.0D;
    private static final int INSPECT_TICKS = 70;

    private final CustomerEntity customer;
    private BlockPos counter;
    private int inspecting;

    public VisitCounterGoal(CustomerEntity customer) {
        this.customer = customer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (customer.hasToured() || customer.getRole() != CustomerRole.NORMAL
                || customer.isFleeing() || customer.isScared()) {
            return false;
        }
        CustomerOrder order = customer.getOrder();
        if (order == null || !order.isComplete() || !(customer.level() instanceof ServerLevel level)
                || level.getServer() == null) {
            return false;
        }
        counter = TycoonData.get(level.getServer())
                .nearestCounter(level.dimension().location(), customer.blockPosition(), SEARCH_RANGE);
        return counter != null;
    }

    @Override
    public boolean canContinueToUse() {
        return counter != null && !customer.hasToured() && !customer.isFleeing() && !customer.isScared();
    }

    @Override
    public void start() {
        inspecting = 0;
        customer.getNavigation().moveTo(counter.getX() + 0.5D, counter.getY(), counter.getZ() + 0.5D, 0.9D);
    }

    @Override
    public void tick() {
        double distSqr = customer.distanceToSqr(counter.getX() + 0.5D, counter.getY() + 0.5D, counter.getZ() + 0.5D);
        if (distSqr > 6.0D) {
            if (customer.getNavigation().isDone()) {
                customer.getNavigation().moveTo(counter.getX() + 0.5D, counter.getY(), counter.getZ() + 0.5D, 0.9D);
            }
            return;
        }
        customer.getNavigation().stop();
        customer.getLookControl().setLookAt(counter.getX() + 0.5D, counter.getY() + 1.0D, counter.getZ() + 0.5D);
        if (inspecting == 0) {
            customer.say(Dialogue.Situation.INSPECT);
        }
        if (++inspecting >= INSPECT_TICKS && customer.level() instanceof ServerLevel level) {
            rate(level);
        }
    }

    private void rate(ServerLevel level) {
        customer.setToured();
        TycoonData data = TycoonData.get(level.getServer());
        FactoryEvaluator.Report report = FactoryEvaluator.evaluate(level, counter);
        int score = FactoryEvaluator.score(report, customer.getTaste(), customer.getPickiness(),
                customer.getGenerosity(), data);
        int stars = FactoryEvaluator.stars(score);
        Player witness = level.getNearestPlayer(customer, 32.0D);

        customer.say(stars >= 4 ? Dialogue.Situation.RATING_HIGH
                : stars == 3 ? Dialogue.Situation.RATING_MID : Dialogue.Situation.RATING_LOW);

        int rep = stars >= 4 ? 1 + RunStats.reputationGain(data) : stars <= 2 ? -1 : 0;
        if (rep != 0) {
            data.changeReputation(rep, witness, "reason.tycoonrogue.rating");
        }
        // Tip: nothing for 1-2 stars, then 2, 4, 6 emeralds, scaled by cards.
        int tip = (int) Math.round(Math.max(0, (stars - 2) * 2) * RunStats.tipMultiplier(data));
        if (tip > 0) {
            data.addRevenue(tip);
            for (ItemStack stack : Currency.stacksFor(tip)) {
                level.addFreshEntity(new ItemEntity(level, counter.getX() + 0.5D, counter.getY() + 1.1D,
                        counter.getZ() + 0.5D, stack));
            }
        }
        Feed.post(level.getServer(), customer, "review." + stars, stars + "");
        if (stars <= 1) {
            Feed.news(level.getServer(), "bad_factory");
        }
        counter = null;
    }
}
