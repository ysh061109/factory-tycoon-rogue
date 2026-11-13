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
    private FactoryEvaluator.Report report;
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
        if (customer.hasToured() || !customer.hasBrowsed() && BrowseGoal.hasStandsFor(customer)
                || customer.getPurchasePrice() > 0 || customer.getRole() != CustomerRole.NORMAL
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
            this.report = report;
            int[] order = switch (customer.getTaste()) {
                // Car lovers: wheels and tyres, then the engine.
                case CARS -> new int[]{FactoryEvaluator.STOP_WHEEL, FactoryEvaluator.STOP_ENGINE,
                        FactoryEvaluator.STOP_BELT};
                // Airplane lovers: propellers, windmill wings, then the balloon or the engine.
                case AIRPLANES -> new int[]{FactoryEvaluator.STOP_PROPELLER, FactoryEvaluator.STOP_SAIL,
                        FactoryEvaluator.STOP_BALLOON, FactoryEvaluator.STOP_ENGINE};
                default -> new int[]{FactoryEvaluator.STOP_BELT, FactoryEvaluator.STOP_WHEEL,
                        FactoryEvaluator.STOP_ROCKET, FactoryEvaluator.STOP_PROPELLER};
            };
            for (int kind : order) {
                List<BlockPos> pool = poolFor(report, kind);
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
            BlockPos first = FactoryEvaluator.walkTarget((ServerLevel) customer.level(), stops.get(0).pos());
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
        ServerLevel level = (ServerLevel) customer.level();
        // Vehicles may hover or fly: the customer stands on the ground below and looks up.
        BlockPos stand = FactoryEvaluator.walkTarget(level, pos);
        boolean vehicle = !stand.equals(pos);
        // Rocket engines are watched from a few steps back.
        double near = vehicle ? 36.0D : stop.kind() == FactoryEvaluator.STOP_ROCKET ? 30.0D
                : stop.kind() == FactoryEvaluator.STOP_MACHINE ? 9.0D
                : stop.kind() == FactoryEvaluator.STOP_BALLOON ? 16.0D : 7.0D;
        double distSqr = vehicle
                ? customer.distanceToSqr(stand.getX() + 0.5D, customer.getY(), stand.getZ() + 0.5D)
                : customer.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (distSqr > near && stuck < 400) {
            stuck++;
            if (customer.getNavigation().isDone()) {
                customer.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 0.8D);
            }
            return;
        }
        customer.getNavigation().stop();
        customer.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (stopWait == 0) {
            watch(level, stop);
        }
        int wait = stop.kind() == FactoryEvaluator.STOP_MACHINE ? 40 : 70;
        if (++stopWait >= wait) {
            stopIndex++;
            stopWait = 0;
            stuck = 0;
            BlockPos next = stopIndex < stops.size() ? FactoryEvaluator.walkTarget(level, stops.get(stopIndex).pos()) : counter;
            customer.getNavigation().moveTo(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D, 0.8D);
        }
    }

    private static List<BlockPos> poolFor(FactoryEvaluator.Report report, int kind) {
        return switch (kind) {
            case FactoryEvaluator.STOP_BELT -> report.belts();
            case FactoryEvaluator.STOP_WHEEL -> report.wheelSpots();
            case FactoryEvaluator.STOP_ROCKET -> report.thrusterSpots();
            case FactoryEvaluator.STOP_PROPELLER -> report.propellerSpots();
            case FactoryEvaluator.STOP_SAIL -> report.sailSpots();
            case FactoryEvaluator.STOP_ENGINE -> report.engineSpots();
            case FactoryEvaluator.STOP_BALLOON -> report.balloonSpots();
            default -> List.of();
        };
    }

    /** Windmill wings only count as working when a propeller or windmill bearing nearby is turning. */
    private boolean sailsTurning(ServerLevel level) {
        if (report == null) {
            return false;
        }
        for (BlockPos bearing : report.propellerSpots()) {
            if (FactoryEvaluator.isLiveAt(level, bearing, FactoryEvaluator.STOP_PROPELLER)) {
                return true;
            }
        }
        return false;
    }

    /** The customer reaches a stop: looks at it, says something and notices whether it is really working. */
    private void watch(ServerLevel level, Stop stop) {
        BlockPos pos = stop.pos();
        int kind = stop.kind();
        boolean live = kind == FactoryEvaluator.STOP_MACHINE
                || (kind == FactoryEvaluator.STOP_SAIL ? sailsTurning(level) : FactoryEvaluator.isLiveAt(level, pos, kind));
        if (kind != FactoryEvaluator.STOP_MACHINE && live) {
            witnessed++;
            Taste taste = customer.getTaste();
            boolean carPart = kind == FactoryEvaluator.STOP_WHEEL || kind == FactoryEvaluator.STOP_ENGINE;
            boolean planePart = kind == FactoryEvaluator.STOP_PROPELLER || kind == FactoryEvaluator.STOP_SAIL
                    || kind == FactoryEvaluator.STOP_BALLOON;
            if ((carPart && taste == Taste.CARS) || (planePart && taste == Taste.AIRPLANES)) {
                lovedLive = true;
            }
        }
        if (kind != FactoryEvaluator.STOP_MACHINE && !live) {
            customer.say(Dialogue.Situation.STOP_IDLE);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 5, 0.2D, 0.1D, 0.2D, 0.01D);
            return;
        }
        level.sendParticles(kind == FactoryEvaluator.STOP_ROCKET || kind == FactoryEvaluator.STOP_BALLOON
                        ? net.minecraft.core.particles.ParticleTypes.FIREWORK
                        : kind == FactoryEvaluator.STOP_PROPELLER || kind == FactoryEvaluator.STOP_SAIL
                        ? net.minecraft.core.particles.ParticleTypes.CLOUD
                        : net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 5, 0.3D, 0.2D, 0.3D, 0.01D);
        switch (kind) {
            case FactoryEvaluator.STOP_BELT -> customer.say(Dialogue.Situation.BELT_WATCH);
            case FactoryEvaluator.STOP_WHEEL -> customer.say(Dialogue.Situation.WHEEL_LOOK);
            case FactoryEvaluator.STOP_ROCKET -> customer.say(Dialogue.Situation.ROCKET_LOOK);
            case FactoryEvaluator.STOP_PROPELLER -> customer.say(Dialogue.Situation.PROPELLER_LOOK);
            case FactoryEvaluator.STOP_SAIL -> customer.say(Dialogue.Situation.SAIL_LOOK);
            case FactoryEvaluator.STOP_ENGINE -> customer.say(Dialogue.Situation.ENGINE_LOOK);
            case FactoryEvaluator.STOP_BALLOON -> customer.say(Dialogue.Situation.BALLOON_LOOK);
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
