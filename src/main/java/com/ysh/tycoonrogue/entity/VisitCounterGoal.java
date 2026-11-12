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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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
    private record Stop(BlockPos pos, int kind) {
    }

    private final List<Stop> stops = new ArrayList<>();
    private int witnessed;
    private boolean lovedLive;
    private int stopIndex;
    private int stopWait;
    private int stuck;

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
        stopIndex = 0;
        stopWait = 0;
        stuck = 0;
        witnessed = 0;
        lovedLive = false;
        stops.clear();
        if (customer.level() instanceof ServerLevel level) {
            // Walk the factory: the belt, the wheels and the rocket engines first (the customer's own
            // passion first), then a machine or two, never more than four stops.
            FactoryEvaluator.Report report = FactoryEvaluator.evaluate(level, counter);
            int[] order = customer.getTaste() == Taste.CARS ? new int[]{FactoryEvaluator.STOP_WHEEL,
                    FactoryEvaluator.STOP_ROCKET, FactoryEvaluator.STOP_BELT}
                    : customer.getTaste() == Taste.AIRPLANES ? new int[]{FactoryEvaluator.STOP_ROCKET,
                    FactoryEvaluator.STOP_WHEEL, FactoryEvaluator.STOP_BELT}
                    : new int[]{FactoryEvaluator.STOP_BELT, FactoryEvaluator.STOP_WHEEL, FactoryEvaluator.STOP_ROCKET};
            for (int kind : order) {
                List<BlockPos> pool = kind == FactoryEvaluator.STOP_BELT ? report.belts()
                        : kind == FactoryEvaluator.STOP_WHEEL ? report.wheelSpots() : report.thrusterSpots();
                if (!pool.isEmpty() && stops.size() < 3) {
                    stops.add(new Stop(pool.get(customer.getRandom().nextInt(pool.size())), kind));
                }
            }
            List<BlockPos> spots = new ArrayList<>(report.spots());
            Collections.shuffle(spots, new java.util.Random(customer.getRandom().nextLong()));
            for (int i = 0; i < spots.size() && stops.size() < 4 && i < 2; i++) {
                stops.add(new Stop(spots.get(i), FactoryEvaluator.STOP_MACHINE));
            }
        }
        if (!stops.isEmpty()) {
            BlockPos first = stops.get(0).pos();
            customer.getNavigation().moveTo(first.getX() + 0.5D, first.getY(), first.getZ() + 0.5D, 0.9D);
            return;
        }
        customer.getNavigation().moveTo(counter.getX() + 0.5D, counter.getY(), counter.getZ() + 0.5D, 0.9D);
    }

    @Override
    public void tick() {
        if (stopIndex < stops.size()) {
            tourStop();
            return;
        }
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

    private void tourStop() {
        Stop stop = stops.get(stopIndex);
        BlockPos pos = stop.pos();
        // Rocket engines are watched from a few steps back.
        double near = stop.kind() == FactoryEvaluator.STOP_ROCKET ? 30.0D
                : stop.kind() == FactoryEvaluator.STOP_MACHINE ? 9.0D : 7.0D;
        double distSqr = customer.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (distSqr > near && stuck < 400) {
            stuck++;
            if (customer.getNavigation().isDone()) {
                customer.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.8D);
            }
            return;
        }
        customer.getNavigation().stop();
        customer.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (stopWait == 0 && customer.level() instanceof ServerLevel level) {
            watch(level, stop);
        }
        int wait = stop.kind() == FactoryEvaluator.STOP_MACHINE ? 40 : 70;
        if (++stopWait >= wait) {
            stopIndex++;
            stopWait = 0;
            stuck = 0;
            BlockPos next = stopIndex < stops.size() ? stops.get(stopIndex).pos() : counter;
            customer.getNavigation().moveTo(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D, 0.8D);
        }
    }

    /** The customer reaches a stop: looks at it, says something and notices whether it is really working. */
    private void watch(ServerLevel level, Stop stop) {
        BlockPos pos = stop.pos();
        boolean live = stop.kind() == FactoryEvaluator.STOP_MACHINE
                || FactoryEvaluator.isLiveAt(level, pos, stop.kind());
        if (stop.kind() != FactoryEvaluator.STOP_MACHINE && live) {
            witnessed++;
            Taste taste = customer.getTaste();
            if ((stop.kind() == FactoryEvaluator.STOP_WHEEL && taste == Taste.CARS)
                    || (stop.kind() == FactoryEvaluator.STOP_ROCKET && taste == Taste.AIRPLANES)) {
                lovedLive = true;
            }
        }
        if (stop.kind() != FactoryEvaluator.STOP_MACHINE && !live) {
            customer.say(Dialogue.Situation.STOP_IDLE);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 5, 0.2D, 0.1D, 0.2D, 0.01D);
            return;
        }
        level.sendParticles(stop.kind() == FactoryEvaluator.STOP_ROCKET
                        ? net.minecraft.core.particles.ParticleTypes.FIREWORK
                        : net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 5, 0.3D, 0.2D, 0.3D, 0.01D);
        switch (stop.kind()) {
            case FactoryEvaluator.STOP_BELT -> customer.say(Dialogue.Situation.BELT_WATCH);
            case FactoryEvaluator.STOP_WHEEL -> customer.say(Dialogue.Situation.WHEEL_LOOK);
            case FactoryEvaluator.STOP_ROCKET -> customer.say(Dialogue.Situation.ROCKET_LOOK);
            default -> {
                if (stopIndex == 0) {
                    customer.say(Dialogue.Situation.INSPECT);
                } else if (stopIndex == stops.size() - 1) {
                    customer.say(Dialogue.Situation.COLOR_TALK, net.minecraft.network.chat.Component.translatable(
                            "color.minecraft." + customer.getFavoriteColor().getName()));
                }
            }
        }
    }

    private void rate(ServerLevel level) {
        customer.setToured();
        TycoonData data = TycoonData.get(level.getServer());
        FactoryEvaluator.Report report = FactoryEvaluator.evaluate(level, counter);
        int colourId = customer.getFavoriteColor().getId();
        int flow = com.ysh.tycoonrogue.event.ProductionTracker.flowScore(level, counter);
        int inspect = Math.min(8, witnessed * 3 + (lovedLive ? 2 : 0));
        FactoryEvaluator.Breakdown parts = FactoryEvaluator.breakdown(report, customer.getTaste(), colourId,
                customer.getPickiness(), customer.getGenerosity(), data, flow, inspect);
        int score = parts.total();
        int stars = FactoryEvaluator.stars(score);
        Player witness = level.getNearestPlayer(customer, 32.0D);

        customer.say(stars >= 4 ? Dialogue.Situation.RATING_HIGH
                : stars == 3 ? Dialogue.Situation.RATING_MID : Dialogue.Situation.RATING_LOW);
        if (flow >= 7) {
            customer.sayLater(Dialogue.Situation.FLOW_GOOD, 70);
        } else if (flow <= 2) {
            customer.sayLater(Dialogue.Situation.FLOW_BAD, 70);
        }
        level.sendParticles(stars >= 3 ? net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER
                        : net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                customer.getX(), customer.getY() + 2.0D, customer.getZ(), 10, 0.4D, 0.3D, 0.4D, 0.0D);
        if (witness instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.tycoonrogue.rated",
                    net.minecraft.network.chat.Component.translatable("job.tycoonrogue." + customer.getJob().id()),
                    "★".repeat(stars) + "☆".repeat(5 - stars), score,
                    net.minecraft.network.chat.Component.translatable(customer.getTaste().translationKey()),
                    FactoryEvaluator.showcaseScore(report, customer.getTaste())), false);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.tycoonrogue.rated.detail", parts.operation(), parts.art(),
                    net.minecraft.network.chat.Component.translatable("color.minecraft." + customer.getFavoriteColor().getName()),
                    parts.colour()), false);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.tycoonrogue.rated.flow", parts.flow(), parts.inspect(), witnessed), false);
        }

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
        Feed.post(level.getServer(), customer, "review." + stars, stars);
        if (stars <= 1) {
            Feed.news(level.getServer(), "bad_factory");
        }
        counter = null;
    }
}
