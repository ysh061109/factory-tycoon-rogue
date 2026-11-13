package com.ysh.tycoonrogue.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about the current run that must survive a restart: reputation, rumor, the round
 * timer and revenue, and the cards picked so far. One instance per world.
 */
public class TycoonData extends SavedData {
    public static final String NAME = "tycoonrogue_run";
    public static final int START_REPUTATION = 50;
    /** One round is one Minecraft day. */
    public static final int ROUND_TICKS = 16800;
    /** This many failed rounds in a row ends the run (cards and round reset). */
    public static final int MAX_FAIL_STREAK = 3;

    private int reputation = START_REPUTATION;
    private int rumor;
    private int round = 1;
    private int ticksLeft = ROUND_TICKS;
    private int revenue;
    private int failStreak;
    private final List<ResourceLocation> cards = new ArrayList<>();
    /** Cards offered at the end of a round, waiting for the player to choose one. */
    private final List<ResourceLocation> pending = new ArrayList<>();
    /** Newest first. Capped at {@link #MAX_FEED} entries. */
    private final List<FeedEntry> feed = new ArrayList<>();
    /** The job that is "trending" this round: its customers pay 30% more. Empty = none yet. */
    private String trendJob = "";
    /** Extra visitors from a streamer visit, 0..100. Fades over time. */
    private int buzz;
    private final List<CounterPos> counters = new ArrayList<>();
    /** Supply Desks (the shop). Customers queue in front of these. */
    private final List<CounterPos> desks = new ArrayList<>();

    public static final int MAX_FEED = 40;

    /** A placed Tycoon Counter (the factory evaluation desk). */
    public record CounterPos(ResourceLocation dimension, BlockPos pos) {
    }

    public static TycoonData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TycoonData::new, TycoonData::load, null), NAME);
    }

    // ---- reputation and rumor ----

    public int getReputation() {
        return reputation;
    }

    /** Changes reputation (0..100). Tells the player why, if one is given. */
    public void changeReputation(int delta, Player witness, String reasonKey) {
        int before = reputation;
        reputation = Math.max(0, Math.min(100, reputation + delta));
        setDirty();
        int applied = reputation - before;
        if (witness != null && applied != 0) {
            witness.displayClientMessage(Component.translatable(
                    "message.tycoonrogue.reputation",
                    (applied > 0 ? "+" : "") + applied,
                    Component.translatable(reasonKey),
                    reputation), true);
        }
    }

    /** Rumor 0..100: how much bad word of mouth is going around. It lowers visitor numbers. */
    public int getRumor() {
        return rumor;
    }

    public void addRumor(int amount) {
        rumor = Math.max(0, Math.min(100, rumor + amount));
        setDirty();
    }

    /** Called every 600 ticks. Bad word of mouth fades slowly. */
    public void decayRumor() {
        if (rumor > 0) {
            rumor = Math.max(0, rumor - 2);
            setDirty();
        }
    }

    // ---- rounds ----

    public int getRound() {
        return round;
    }

    public int getTicksLeft() {
        return ticksLeft;
    }

    public int getRevenue() {
        return revenue;
    }

    /** Emeralds that must be earned this round: 40, then 30 more every round, changed by cards. */
    public int getQuota() {
        return (int) Math.round((40.0D + 30.0D * (round - 1)) * RunStats.quotaMultiplier(this));
    }

    public void addRevenue(int emeralds) {
        if (emeralds > 0) {
            revenue += emeralds;
            setDirty();
        }
    }

    public void tickRound(int ticks) {
        ticksLeft = Math.max(0, ticksLeft - ticks);
        setDirty();
    }

    public void forceRoundEnd() {
        ticksLeft = 0;
        setDirty();
    }

    /** Moves to the next round with a fresh timer and revenue. */
    public void startNextRound() {
        round++;
        ticksLeft = ROUND_TICKS;
        revenue = 0;
        setDirty();
    }

    public int getFailStreak() {
        return failStreak;
    }

    public void setFailStreak(int streak) {
        failStreak = streak;
        setDirty();
    }

    /** The run is over: forget cards and start again from round 1. */
    public void resetRun() {
        round = 1;
        ticksLeft = ROUND_TICKS;
        revenue = 0;
        failStreak = 0;
        reputation = START_REPUTATION;
        rumor = 0;
        cards.clear();
        pending.clear();
        feed.clear();
        trendJob = "";
        buzz = 0;
        setDirty();
    }

    // ---- cards ----

    public List<ResourceLocation> getCards() {
        return cards;
    }

    public List<ResourceLocation> getPending() {
        return pending;
    }

    /** Rerolls used for the card choice that is waiting (not saved; a new choice resets it). */
    private int rerolls;

    public int getRerolls() {
        return rerolls;
    }

    public void addReroll() {
        rerolls++;
    }

    public void setPending(List<ResourceLocation> offered) {
        rerolls = 0;
        pending.clear();
        pending.addAll(offered);
        setDirty();
    }

    /** Takes one of the offered cards. Returns false if it was not on offer. */
    public boolean pick(ResourceLocation card) {
        if (!pending.contains(card)) {
            return false;
        }
        cards.add(card);
        pending.clear();
        setDirty();
        return true;
    }

    // ---- feed, trend, buzz ----

    public List<FeedEntry> getFeed() {
        return feed;
    }

    public void addFeed(FeedEntry entry) {
        feed.add(0, entry);
        while (feed.size() > MAX_FEED) {
            feed.remove(feed.size() - 1);
        }
        setDirty();
    }

    public String getTrendJob() {
        return trendJob;
    }

    public void setTrendJob(String job) {
        trendJob = job;
        setDirty();
    }

    public int getBuzz() {
        return buzz;
    }

    public void addBuzz(int amount) {
        buzz = Math.max(0, Math.min(100, buzz + amount));
        setDirty();
    }

    public void decayBuzz() {
        if (buzz > 0) {
            buzz = Math.max(0, buzz - 5);
            setDirty();
        }
    }

    // ---- supply desks (shops) ----

    public void addDesk(ResourceLocation dimension, BlockPos pos) {
        CounterPos d = new CounterPos(dimension, pos.immutable());
        if (!desks.contains(d)) {
            desks.add(d);
            setDirty();
        }
    }

    public void removeDesk(ResourceLocation dimension, BlockPos pos) {
        if (desks.remove(new CounterPos(dimension, pos.immutable()))) {
            setDirty();
        }
    }

    public BlockPos nearestDesk(ResourceLocation dimension, BlockPos from, double maxDistance) {
        BlockPos best = null;
        double bestDistance = maxDistance * maxDistance;
        for (CounterPos d : desks) {
            if (d.dimension().equals(dimension) && d.pos().distSqr(from) <= bestDistance) {
                bestDistance = d.pos().distSqr(from);
                best = d.pos();
            }
        }
        return best;
    }

    /** Where customers queue: the nearest Supply Desk, or the Tycoon Counter if there is no desk. */
    public BlockPos nearestShop(ResourceLocation dimension, BlockPos from, double maxDistance) {
        BlockPos desk = nearestDesk(dimension, from, maxDistance);
        return desk != null ? desk : nearestCounter(dimension, from, maxDistance);
    }

    // ---- counters ----

    public void addCounter(ResourceLocation dimension, BlockPos pos) {
        CounterPos counter = new CounterPos(dimension, pos.immutable());
        if (!counters.contains(counter)) {
            counters.add(counter);
            setDirty();
        }
    }

    public void removeCounter(ResourceLocation dimension, BlockPos pos) {
        if (counters.remove(new CounterPos(dimension, pos.immutable()))) {
            setDirty();
        }
    }

    /** Every counter as dimension + position, for systems that watch the factories over time. */
    public List<net.minecraft.core.GlobalPos> allCounters() {
        List<net.minecraft.core.GlobalPos> list = new ArrayList<>();
        for (CounterPos counter : counters) {
            list.add(net.minecraft.core.GlobalPos.of(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                            counter.dimension()), counter.pos()));
        }
        return list;
    }

    /** The closest counter in this dimension within {@code maxDistance} blocks, or null. */
    public BlockPos nearestCounter(ResourceLocation dimension, BlockPos from, double maxDistance) {
        BlockPos best = null;
        double bestDistance = maxDistance * maxDistance;
        for (CounterPos counter : counters) {
            if (counter.dimension().equals(dimension)) {
                double distance = counter.pos().distSqr(from);
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = counter.pos();
                }
            }
        }
        return best;
    }

    // ---- saving ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Reputation", reputation);
        tag.putInt("Rumor", rumor);
        tag.putInt("Round", round);
        tag.putInt("TicksLeft", ticksLeft);
        tag.putInt("Revenue", revenue);
        tag.putInt("FailStreak", failStreak);
        tag.put("Cards", toList(cards));
        tag.put("Pending", toList(pending));
        ListTag feedTag = new ListTag();
        for (FeedEntry entry : feed) {
            feedTag.add(entry.save());
        }
        tag.put("Feed", feedTag);
        tag.putString("TrendJob", trendJob);
        tag.putInt("Buzz", buzz);
        ListTag counterTag = new ListTag();
        for (CounterPos counter : counters) {
            CompoundTag c = new CompoundTag();
            c.putString("Dim", counter.dimension().toString());
            c.putInt("X", counter.pos().getX());
            c.putInt("Y", counter.pos().getY());
            c.putInt("Z", counter.pos().getZ());
            counterTag.add(c);
        }
        tag.put("Counters", counterTag);
        ListTag deskTag = new ListTag();
        for (CounterPos d : desks) {
            CompoundTag c = new CompoundTag();
            c.putString("Dim", d.dimension().toString());
            c.putInt("X", d.pos().getX());
            c.putInt("Y", d.pos().getY());
            c.putInt("Z", d.pos().getZ());
            deskTag.add(c);
        }
        tag.put("Desks", deskTag);
        return tag;
    }

    public static TycoonData load(CompoundTag tag, HolderLookup.Provider registries) {
        TycoonData data = new TycoonData();
        data.reputation = tag.contains("Reputation") ? tag.getInt("Reputation") : START_REPUTATION;
        data.rumor = tag.getInt("Rumor");
        data.round = Math.max(1, tag.getInt("Round"));
        data.ticksLeft = tag.contains("TicksLeft") ? tag.getInt("TicksLeft") : ROUND_TICKS;
        data.revenue = tag.getInt("Revenue");
        data.failStreak = tag.getInt("FailStreak");
        readList(tag.getList("Cards", Tag.TAG_STRING), data.cards);
        readList(tag.getList("Pending", Tag.TAG_STRING), data.pending);
        ListTag feedTag = tag.getList("Feed", Tag.TAG_COMPOUND);
        for (int i = 0; i < feedTag.size(); i++) {
            data.feed.add(FeedEntry.load(feedTag.getCompound(i)));
        }
        data.trendJob = tag.getString("TrendJob");
        data.buzz = tag.getInt("Buzz");
        ListTag counterTag = tag.getList("Counters", Tag.TAG_COMPOUND);
        for (int i = 0; i < counterTag.size(); i++) {
            CompoundTag c = counterTag.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(c.getString("Dim"));
            if (dim != null) {
                data.counters.add(new CounterPos(dim, new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z"))));
            }
        }
        ListTag deskTag = tag.getList("Desks", Tag.TAG_COMPOUND);
        for (int i = 0; i < deskTag.size(); i++) {
            CompoundTag c = deskTag.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(c.getString("Dim"));
            if (dim != null) {
                data.desks.add(new CounterPos(dim, new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z"))));
            }
        }
        return data;
    }

    private static ListTag toList(List<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static void readList(ListTag list, List<ResourceLocation> into) {
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                into.add(id);
            }
        }
    }
}
